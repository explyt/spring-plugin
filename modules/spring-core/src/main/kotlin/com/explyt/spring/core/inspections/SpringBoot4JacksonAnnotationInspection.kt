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
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import org.jetbrains.uast.UClass

/**
 * Reports Spring Boot's `@JsonComponent` / `@JsonMixin` annotations, renamed in Spring Boot 4.0, and offers a
 * quick-fix that replaces them with `@JacksonComponent` / `@JacksonMixin` (same `org.springframework.boot.jackson`
 * package), preserving any declared attributes.
 *
 * Only runs in a Spring Boot 4+ project.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
class SpringBoot4JacksonAnnotationInspection : Spring4UastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return super.isAvailableForFile(file) && isAnyClassAvailable(file, *RENAMES.keys.toTypedArray())
    }

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor?> {
        val problems = mutableListOf<ProblemDescriptor>()
        for ((oldFqn, newFqn) in RENAMES) {
            val uAnnotation = uClass.uAnnotations.firstOrNull { it.qualifiedName == oldFqn } ?: continue
            val highlightElement = uAnnotation.sourcePsi ?: continue
            val newShortName = newFqn.substringAfterLast('.')

            // Preserve the declared attributes by reconstructing them on the new annotation.
            val javaAnnotation = uClass.javaPsi.modifierList?.findAnnotation(oldFqn)
            val attributes = reconstructAttributes(uClass.javaPsi, newFqn, javaAnnotation)
            val fix = RewriteAnnotationQuickFix(newFqn, uClass.javaPsi, attributes, oldFqn)

            problems += manager.createProblemDescriptor(
                highlightElement,
                message("explyt.spring.inspection.boot4.jackson", newShortName),
                isOnTheFly,
                arrayOf<LocalQuickFix>(fix),
                ProblemHighlightType.LIKE_DEPRECATED
            )
        }
        return problems.toTypedArray()
    }

    private fun reconstructAttributes(
        owner: PsiClass,
        newFqn: String,
        oldAnnotation: PsiAnnotation?
    ): Array<com.intellij.psi.PsiNameValuePair> {
        val argsText = oldAnnotation?.parameterList?.attributes
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { it.text }
            ?: return com.intellij.psi.PsiNameValuePair.EMPTY_ARRAY
        return runCatching {
            val factory = com.intellij.psi.JavaPsiFacade.getInstance(owner.project).elementFactory
            factory.createAnnotationFromText("@$newFqn($argsText)", owner).parameterList.attributes
        }.getOrDefault(com.intellij.psi.PsiNameValuePair.EMPTY_ARRAY)
    }

    companion object {
        private const val JSON_COMPONENT = "org.springframework.boot.jackson.JsonComponent"
        private const val JACKSON_COMPONENT = "org.springframework.boot.jackson.JacksonComponent"
        private const val JSON_MIXIN = "org.springframework.boot.jackson.JsonMixin"
        private const val JACKSON_MIXIN = "org.springframework.boot.jackson.JacksonMixin"

        // old annotation FQN -> new annotation FQN
        private val RENAMES: Map<String, String> = mapOf(
            JSON_COMPONENT to JACKSON_COMPONENT,
            JSON_MIXIN to JACKSON_MIXIN,
        )
    }
}
