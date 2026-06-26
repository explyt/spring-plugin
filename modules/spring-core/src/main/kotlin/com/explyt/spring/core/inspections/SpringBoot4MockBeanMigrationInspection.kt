/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.inspections.quickfix.RewriteAnnotationQuickFix
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UField
import org.jetbrains.uast.getParentOfType

/**
 * Reports Spring Boot's `@MockBean` / `@SpyBean` test annotations, removed in Spring Boot 4.0, and offers a
 * quick-fix that replaces them with Spring Framework's `@MockitoBean` / `@MockitoSpyBean`.
 *
 * Only runs in a Spring Boot 4+ project.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
class SpringBoot4MockBeanMigrationInspection : Spring4UastLocalInspectionTool() {

    override fun checkField(field: UField, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        for ((oldFqn, newFqn) in REPLACEMENTS) {
            val annotation = field.findAnnotation(oldFqn) ?: continue
            problems += createProblem(annotation, oldFqn, newFqn, manager, isOnTheFly) ?: continue
        }
        return problems.toTypedArray()
    }

    private fun createProblem(
        annotation: UAnnotation,
        oldFqn: String,
        newFqn: String,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): ProblemDescriptor? {
        val highlightElement = annotation.sourcePsi ?: return null
        // The annotation owner is the annotated field; RewriteAnnotationQuickFix removes the old annotation and
        // adds the new one (handling imports) for both Java and Kotlin via the field's light PsiModifierListOwner.
        val owner = annotation.getParentOfType<UField>()?.javaPsi as? PsiModifierListOwner ?: return null
        val newShortName = newFqn.substringAfterLast('.')
        return manager.createProblemDescriptor(
            highlightElement,
            message("explyt.spring.inspection.boot4.mockbean", newShortName),
            isOnTheFly,
            arrayOf<LocalQuickFix>(RewriteAnnotationQuickFix(newFqn, owner, oldFqn)),
            ProblemHighlightType.LIKE_DEPRECATED
        )
    }

    companion object {
        private const val MOCK_BEAN = "org.springframework.boot.test.mock.mockito.MockBean"
        private const val SPY_BEAN = "org.springframework.boot.test.mock.mockito.SpyBean"
        private const val MOCKITO_BEAN = "org.springframework.test.context.bean.override.mockito.MockitoBean"
        private const val MOCKITO_SPY_BEAN = "org.springframework.test.context.bean.override.mockito.MockitoSpyBean"

        // old annotation FQN -> new annotation FQN
        private val REPLACEMENTS: Map<String, String> = mapOf(
            MOCK_BEAN to MOCKITO_BEAN,
            SPY_BEAN to MOCKITO_SPY_BEAN,
        )
    }
}
