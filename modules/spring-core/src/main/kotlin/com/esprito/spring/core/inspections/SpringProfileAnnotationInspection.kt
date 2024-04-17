package com.esprito.spring.core.inspections

import com.esprito.inspection.SpringBaseUastLocalInspectionTool
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.language.profiles.ProfilesLanguage
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
                    sourcePsi, SpringCoreBundle.message("esprito.spring.inspection.profile.empty"),
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