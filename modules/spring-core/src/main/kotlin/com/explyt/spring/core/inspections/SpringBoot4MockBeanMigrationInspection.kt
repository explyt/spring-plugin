/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.inspections.quickfix.ReplaceAnnotationQuickFix
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField

/**
 * Reports Spring Boot's `@MockBean` / `@SpyBean` test annotations, removed in Spring Boot 4.0, and offers a
 * quick-fix that replaces them with Spring Framework's `@MockitoBean` / `@MockitoSpyBean`.
 *
 * Both the legacy and the replacement annotations are applicable to fields **and** types, so both positions are
 * inspected.
 *
 * Only runs in a Spring Boot 4+ project.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
class SpringBoot4MockBeanMigrationInspection : Spring4UastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return super.isAvailableForFile(file) && isAnyClassAvailable(file, MOCK_BEAN, SPY_BEAN)
    }

    override fun checkField(field: UField, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        return checkAnnotationOwner(field.uAnnotations, manager, isOnTheFly)
    }

    override fun checkClass(uClass: UClass, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        return checkAnnotationOwner(uClass.uAnnotations, manager, isOnTheFly)
    }

    private fun checkAnnotationOwner(
        annotations: List<UAnnotation>,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        for (annotation in annotations) {
            val replacement = REPLACEMENTS[annotation.qualifiedName] ?: continue
            problems += createProblem(annotation, replacement, manager, isOnTheFly) ?: continue
        }
        return problems.toTypedArray()
    }

    private fun createProblem(
        annotation: UAnnotation,
        replacement: Replacement,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): ProblemDescriptor? {
        // Highlight (and rewrite) the annotation source PSI: the UAST `javaPsi` owner is light PSI in Kotlin, which
        // the platform add-annotation machinery rejects, leaving the reported problem without any usable fix.
        val highlightElement = annotation.sourcePsi ?: return null
        val newShortName = replacement.newFqn.substringAfterLast('.')

        // An attribute without a safe counterpart must never be silently dropped: report, but withhold the fix.
        val unmappedAttribute = ReplaceAnnotationQuickFix
            .unmappedAttributes(annotation, replacement.attributeRenames).firstOrNull()
        if (unmappedAttribute != null) {
            return manager.createProblemDescriptor(
                highlightElement,
                message("explyt.spring.inspection.boot4.mockbean.manual", newShortName, unmappedAttribute),
                isOnTheFly,
                LocalQuickFix.EMPTY_ARRAY,
                ProblemHighlightType.LIKE_DEPRECATED
            )
        }

        return manager.createProblemDescriptor(
            highlightElement,
            message("explyt.spring.inspection.boot4.mockbean", newShortName),
            isOnTheFly,
            arrayOf<LocalQuickFix>(ReplaceAnnotationQuickFix(replacement.newFqn, replacement.attributeRenames)),
            ProblemHighlightType.LIKE_DEPRECATED
        )
    }

    private class Replacement(val newFqn: String, val attributeRenames: Map<String, String>)

    companion object {
        private const val MOCK_BEAN = "org.springframework.boot.test.mock.mockito.MockBean"
        private const val SPY_BEAN = "org.springframework.boot.test.mock.mockito.SpyBean"
        private const val MOCKITO_BEAN = "org.springframework.test.context.bean.override.mockito.MockitoBean"
        private const val MOCKITO_SPY_BEAN = "org.springframework.test.context.bean.override.mockito.MockitoSpyBean"

        /**
         * Attribute mappings verified against `spring-boot-test` 3.5 and `spring-test` 7.0 bytecode.
         *
         * `MockBean`/`SpyBean` select the mocked type through `Class<?>[] value()` / `classes()`, which
         * `MockitoBean`/`MockitoSpyBean` renamed to `types()` (their own `value()` is the bean *name*, a String).
         * `MockBean.answer()` became `MockitoBean.answers()`.
         *
         * Deliberately unmapped, so the fix is withheld rather than producing broken code:
         * - `reset` - the enum type itself moved (`o.s.b.test.mock.mockito.MockReset` ->
         *   `o.s.test.context.bean.override.mockito.MockReset`), so the value expression needs rewriting too;
         * - `SpyBean.proxyTargetAware` - no counterpart on `MockitoSpyBean`;
         * - `SpyBean.serializable`/`extraInterfaces` - not declared on `MockitoSpyBean`.
         */
        private val MOCK_BEAN_RENAMES: Map<String, String> = mapOf(
            "value" to "types",
            "classes" to "types",
            "name" to "name",
            "extraInterfaces" to "extraInterfaces",
            "answer" to "answers",
            "serializable" to "serializable",
        )

        private val SPY_BEAN_RENAMES: Map<String, String> = mapOf(
            "value" to "types",
            "classes" to "types",
            "name" to "name",
        )

        // old annotation FQN -> replacement
        private val REPLACEMENTS: Map<String, Replacement> = mapOf(
            MOCK_BEAN to Replacement(MOCKITO_BEAN, MOCK_BEAN_RENAMES),
            SPY_BEAN to Replacement(MOCKITO_SPY_BEAN, SPY_BEAN_RENAMES),
        )
    }
}
