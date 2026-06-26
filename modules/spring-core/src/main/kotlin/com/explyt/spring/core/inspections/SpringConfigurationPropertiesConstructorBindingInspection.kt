/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringCoreClasses.CONFIGURATION_PROPERTIES
import com.explyt.spring.core.SpringCoreClasses.CONSTRUCTOR_BINDING
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.uast.UClass

/**
 * Reports a redundant `@ConstructorBinding` on a single-constructor `@ConfigurationProperties` class.
 *
 * Since Spring Boot 3.0 `@ConstructorBinding` is no longer needed when a class (or record) has a single constructor:
 * such a class is bound through its constructor automatically. The annotation is only meaningful when there are
 * multiple constructors and one of them must be selected for binding.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide#constructingbinding-no-longer-needed-at-the-type-level">Spring Boot 3.0 Migration Guide</a>
 */
class SpringConfigurationPropertiesConstructorBindingInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor?> {
        if (uClass.lang != KotlinLanguage.INSTANCE) return emptyArray()
        val javaPsi = uClass.javaPsi
        if (!javaPsi.isMetaAnnotatedBy(CONFIGURATION_PROPERTIES)) return emptyArray()
        // The annotation only becomes redundant for a single-constructor class on Spring Boot 3.0+.
        if (javaPsi.constructors.size > 1) return emptyArray()
        if (!SpringBootUtil.isAtLeastSpringBoot3(javaPsi)) return emptyArray()

        val problems = mutableListOf<ProblemDescriptor>()
        for (annotationPsi in findRedundantConstructorBindingAnnotations(uClass)) {
            problems += manager.createProblemDescriptor(
                annotationPsi,
                message("explyt.spring.inspection.kotlin.constructor.binding.redundant"),
                isOnTheFly,
                arrayOf<LocalQuickFix>(RemoveRedundantConstructorBindingFix()),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        }
        return problems.toTypedArray()
    }

    private fun findRedundantConstructorBindingAnnotations(uClass: UClass): List<PsiElement> {
        val classAnnotations = uClass.uAnnotations
            .filter { it.qualifiedName == CONSTRUCTOR_BINDING }
            .mapNotNull { it.sourcePsi }
        val constructorAnnotations = uClass.methods.asSequence()
            .filter { it.isConstructor }
            .flatMap { it.uAnnotations.asSequence() }
            .filter { it.qualifiedName == CONSTRUCTOR_BINDING }
            .mapNotNull { it.sourcePsi }
            .toList()

        return classAnnotations + constructorAnnotations
    }
}

private class RemoveRedundantConstructorBindingFix : LocalQuickFix {

    override fun getFamilyName(): String =
        message("explyt.spring.inspection.kotlin.constructor.binding.redundant.fix")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element: PsiElement = descriptor.psiElement ?: return
        if (!element.isValid) return
        // Capture the enclosing primary constructor before the annotation is removed so the now-redundant
        // `constructor` keyword (only required because of the annotation) can be dropped afterwards.
        val primaryConstructor = (element as? KtAnnotationEntry)?.parentOfType<KtPrimaryConstructor>()
        element.delete()
        primaryConstructor?.removeRedundantConstructorKeywordAndSpace()
    }
}
