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
import org.jetbrains.uast.UMethod

/**
 * Reports the use of Spring's `org.springframework.lang.Nullable` on parameters of Actuator endpoint operation
 * methods (`@ReadOperation` / `@WriteOperation` / `@DeleteOperation`) in Spring Boot 4+ projects, and offers a
 * quick-fix that replaces it with JSpecify's `org.jspecify.annotations.Nullable`.
 *
 * In Spring Boot 4.0 Actuator endpoint nullable parameters use JSpecify's `@Nullable`.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
class SpringBoot4ActuatorNullableInspection : Spring4UastLocalInspectionTool() {

    override fun checkMethod(method: UMethod, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        if (OPERATION_ANNOTATIONS.none { method.findAnnotation(it) != null }) return emptyArray()

        val problems = mutableListOf<ProblemDescriptor>()
        for (parameter in method.uastParameters) {
            val nullable = parameter.findAnnotation(SPRING_NULLABLE) ?: continue
            val highlightElement = nullable.sourcePsi ?: continue
            val owner = parameter.javaPsi as? PsiModifierListOwner ?: continue

            problems += manager.createProblemDescriptor(
                highlightElement,
                message("explyt.spring.inspection.boot4.actuator.nullable"),
                isOnTheFly,
                arrayOf<LocalQuickFix>(RewriteAnnotationQuickFix(JSPECIFY_NULLABLE, owner, SPRING_NULLABLE)),
                ProblemHighlightType.LIKE_DEPRECATED
            )
        }
        return problems.toTypedArray()
    }

    companion object {
        private const val SPRING_NULLABLE = "org.springframework.lang.Nullable"
        private const val JSPECIFY_NULLABLE = "org.jspecify.annotations.Nullable"

        private val OPERATION_ANNOTATIONS = listOf(
            "org.springframework.boot.actuate.endpoint.annotation.ReadOperation",
            "org.springframework.boot.actuate.endpoint.annotation.WriteOperation",
            "org.springframework.boot.actuate.endpoint.annotation.DeleteOperation",
        )
    }
}
