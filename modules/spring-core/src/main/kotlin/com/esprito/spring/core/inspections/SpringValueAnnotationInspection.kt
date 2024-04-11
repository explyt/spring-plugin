package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.esprito.spring.core.completion.properties.DefinedConfigurationProperty
import com.esprito.spring.core.inspections.quickfix.ReplacementStringQuickFix
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.evaluateString


class SpringValueAnnotationInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        return method.uastParameters
            .mapNotNull { it.findAnnotation(SpringCoreClasses.VALUE) }
            .mapNotNull { findProblemDescriptors(it, manager, isOnTheFly) }
            .toTypedArray()
    }

    override fun checkField(field: UField, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        val annotation = field.findAnnotation(SpringCoreClasses.VALUE) ?: return emptyArray()
        return findProblemDescriptors(annotation, manager, isOnTheFly)?.let { arrayOf(it) } ?: emptyArray()
    }

    private fun findProblemDescriptors(
        annotation: UAnnotation, manager: InspectionManager, isOnTheFly: Boolean
    ): ProblemDescriptor? {
        val attributeValue = annotation.findAttributeValue(SpringProperties.VALUE) ?: return null
        val sourcePsi = attributeValue.sourcePsi ?: return null
        val valueText = attributeValue.evaluateString() ?: return null
        if (valueText.isEmpty() || valueText.startsWith("\${") || valueText.startsWith("#{")) return null
        val module = ModuleUtilCore.findModuleForPsiElement(sourcePsi) ?: return null

        val findProperties = DefinedConfigurationPropertiesSearch.getInstance(sourcePsi.project)
            .findProperties(module, valueText)
        val property = findProperties.find { it.key == valueText } ?: return null

        val fixes = getReplacementStringQuickFix(property, sourcePsi, valueText)
        return manager.createProblemDescriptor(
            sourcePsi, SpringCoreBundle.message("esprito.spring.inspection.value.prefix"),
            isOnTheFly, fixes, ProblemHighlightType.WARNING
        )
    }

    private fun getReplacementStringQuickFix(
        property: DefinedConfigurationProperty,
        sourcePsi: PsiElement,
        valueText: String
    ): Array<ReplacementStringQuickFix> {
        val valueTextFromManipulator = ElementManipulators.getValueText(sourcePsi)
        if (valueTextFromManipulator != valueText) return emptyArray()
        return arrayOf(ReplacementStringQuickFix(property.key, "\${" + property.key + "}", sourcePsi))
    }
}