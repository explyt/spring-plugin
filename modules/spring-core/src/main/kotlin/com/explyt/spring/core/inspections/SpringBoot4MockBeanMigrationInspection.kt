/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.inspections.quickfix.ReplaceAnnotationQuickFix
import com.explyt.spring.core.util.SpringBootUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField

/**
 * Reports Spring Boot's `@MockBean` / `@SpyBean` test annotations and offers a quick-fix that replaces them with
 * Spring Framework's `@MockitoBean` / `@MockitoSpyBean`.
 *
 * Covers the whole migration window, not only the state after the removal: Spring Boot 3.4 ships Spring Framework 6.2,
 * which is where the replacements appear and where the legacy annotations are deprecated for removal in 4.0. On 3.4/3.5
 * the platform already reports the deprecation, but a Java `@Deprecated` carries no replacement, so what this
 * inspection adds there is the name of the replacement plus the quick-fix - reported as a weak warning to avoid a
 * second prominent highlight on the same annotation. Once Spring Boot 4 removes the annotations, nothing else reports
 * the stale source and the full-strength highlight applies.
 *
 * Both the legacy and the replacement annotations are applicable to fields **and** types, so both positions are
 * inspected.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
class SpringBoot4MockBeanMigrationInspection : Spring4UastLocalInspectionTool() {

    override fun isSupportedBootVersion(file: PsiFile): Boolean = SpringBootUtil.isAtLeastSpringBoot34(file)

    override fun isAvailableForFile(file: PsiFile): Boolean {
        // Only the replacements have to be resolvable: the Boot 4 upgrade removes the legacy annotations, and the
        // stale source that still uses them is exactly what must be reported.
        return super.isAvailableForFile(file) && isAnyClassAvailable(file, MOCKITO_BEAN, MOCKITO_SPY_BEAN)
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
            val legacyFqn = legacyAnnotationFqn(annotation, REPLACEMENTS.keys) ?: continue
            val replacement = REPLACEMENTS[legacyFqn] ?: continue
            problems += createProblem(annotation, legacyFqn, replacement, manager, isOnTheFly) ?: continue
        }
        return problems.toTypedArray()
    }

    private fun createProblem(
        annotation: UAnnotation,
        legacyFqn: String,
        replacement: Replacement,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): ProblemDescriptor? {
        // Highlight (and rewrite) the annotation source PSI: the UAST `javaPsi` owner is light PSI in Kotlin, which
        // the platform add-annotation machinery rejects, leaving the reported problem without any usable fix.
        val highlightElement = annotation.sourcePsi ?: return null
        val newShortName = replacement.newFqn.substringAfterLast('.')

        // A legacy annotation that still resolves means the deprecation window (3.4/3.5) rather than the Boot 4
        // removal - a per-annotation fact, so the version is not queried again here.
        val stillOnClasspath = annotation.resolve() != null
        val highlightType =
            if (stillOnClasspath) ProblemHighlightType.WEAK_WARNING else ProblemHighlightType.LIKE_DEPRECATED
        val keySuffix = if (stillOnClasspath) DEPRECATED_KEY_SUFFIX else ""

        // An attribute without a safe counterpart must never be silently dropped: report, but withhold the fix.
        val unmappedAttribute = ReplaceAnnotationQuickFix
            .unmappedAttributes(annotation, replacement.attributeRenames).firstOrNull()
        if (unmappedAttribute != null) {
            return manager.createProblemDescriptor(
                highlightElement,
                message("explyt.spring.inspection.boot4.mockbean$keySuffix.manual", newShortName, unmappedAttribute),
                isOnTheFly,
                LocalQuickFix.EMPTY_ARRAY,
                highlightType
            )
        }

        return manager.createProblemDescriptor(
            highlightElement,
            message("explyt.spring.inspection.boot4.mockbean$keySuffix", newShortName),
            isOnTheFly,
            arrayOf<LocalQuickFix>(
                ReplaceAnnotationQuickFix(replacement.newFqn, replacement.attributeRenames, oldFqn = legacyFqn)
            ),
            highlightType
        )
    }

    private class Replacement(val newFqn: String, val attributeRenames: Map<String, String>)

    companion object {
        private const val DEPRECATED_KEY_SUFFIX = ".deprecated"
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
