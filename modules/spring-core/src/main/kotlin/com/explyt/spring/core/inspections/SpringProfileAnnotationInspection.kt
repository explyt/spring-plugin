/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.language.profiles.ProfilesLanguage
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFileFactory
import org.jetbrains.uast.*


class SpringProfileAnnotationInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val annotation = uClass.findAnnotation(SpringCoreClasses.PROFILE) ?: return null
        return problemDescriptors(annotation, manager, isOnTheFly)
    }

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val annotation = method.findAnnotation(SpringCoreClasses.PROFILE) ?: return null
        return problemDescriptors(annotation, manager, isOnTheFly)
    }

    private fun problemDescriptors(
        annotation: UAnnotation, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val attributeValue = annotation.findAttributeValue(SpringProperties.VALUE) ?: return null
        return when (attributeValue) {
            is UCallExpression -> {
                attributeValue.valueArguments
                    .filterIsInstance<ULiteralExpression>()
                    .flatMap { collectProblems(it, manager, isOnTheFly) }
                    .toTypedArray()
            }

            is ULiteralExpression -> collectProblems(attributeValue, manager, isOnTheFly).toTypedArray()

            else -> emptyArray()

        }
    }

    private fun collectProblems(
        attributeValue: ULiteralExpression, manager: InspectionManager, isOnTheFly: Boolean
    ): List<ProblemDescriptor> {
        val sourcePsi = attributeValue.sourcePsi ?: return listOf()
        val valueText = ElementManipulators.getValueText(sourcePsi)
        if (valueText.isEmpty()) {
            return listOf(
                manager.createProblemDescriptor(
                    sourcePsi, SpringCoreBundle.message("explyt.spring.inspection.profile.empty"),
                    isOnTheFly, emptyArray(), ProblemHighlightType.GENERIC_ERROR
                )
            )
        }

        val errorElements = PsiFileFactory.getInstance(manager.project)
            .createFileFromText(ProfilesLanguage.INSTANCE, valueText).children
            .filterIsInstance<PsiErrorElement>()
        if (errorElements.isEmpty()) return emptyList()
        val referenceOffset = getElementOffset(sourcePsi, attributeValue)
        val problems = ArrayList<ProblemDescriptor>(errorElements.size)
        for (errorElement in errorElements) {
            val errorRange = errorElement.textRange
            problems += manager.createProblemDescriptor(
                sourcePsi, errorRange.shiftRight(referenceOffset),
                errorElement.errorDescription, ProblemHighlightType.GENERIC_ERROR, isOnTheFly
            )
        }
        return problems
    }

    private fun getElementOffset(sourcePsi: PsiElement, annotation: UElement): Int {
        if (annotation.lang != JavaLanguage.INSTANCE) return 0
        return try {
            ElementManipulators.getOffsetInElement(sourcePsi)
        } catch (e: Exception) {
            0
        }
    }
}