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
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.inspections.quickfix.ReplacementStringQuickFix
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


class SpringValueAnnotationInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val scheduledAnnotation = method.findAnnotation(SpringCoreClasses.SCHEDULED)
        val scheduledProblems = processScheduled(scheduledAnnotation, manager, isOnTheFly)

        return method.uastParameters
            .mapNotNull { it.findAnnotation(SpringCoreClasses.VALUE) }
            .mapNotNull { findProblemDescriptors(it, manager, isOnTheFly, SpringProperties.VALUE) }
            .toTypedArray() + scheduledProblems
    }

    private fun processScheduled(
        scheduledAnnotation: UAnnotation?, manager: InspectionManager, isOnTheFly: Boolean
    ): List<ProblemDescriptor> {
        if (scheduledAnnotation == null) return emptyList()
        val result = mutableListOf<ProblemDescriptor>()
        findProblemDescriptors(
            scheduledAnnotation, manager, isOnTheFly, SpringScheduledInspection.CRON_PARAM
        )?.let { result.add(it) }
        findProblemDescriptors(
            scheduledAnnotation, manager, isOnTheFly, SpringScheduledInspection.FIXED_RATE_STRING
        )?.let { result.add(it) }
        findProblemDescriptors(
            scheduledAnnotation, manager, isOnTheFly, SpringScheduledInspection.FIXED_RATE_STRING
        )?.let { result.add(it) }
        findProblemDescriptors(
            scheduledAnnotation, manager, isOnTheFly, SpringScheduledInspection.INITIAL_DELAY_STRING
        )?.let { result.add(it) }
        return result
    }

    override fun checkField(field: UField, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        val annotation = field.findAnnotation(SpringCoreClasses.VALUE) ?: return emptyArray()
        return findProblemDescriptors(annotation, manager, isOnTheFly, "value")?.let { arrayOf(it) } ?: emptyArray()
    }

    private fun findProblemDescriptors(
        annotation: UAnnotation, manager: InspectionManager, isOnTheFly: Boolean, annoParameterName: String
    ): ProblemDescriptor? {
        val attributeValue = annotation.findAttributeValue(annoParameterName) ?: return null
        val sourcePsi = attributeValue.sourcePsi ?: return null
        val valueText = attributeValue.evaluateString() ?: return null
        if (valueText.isEmpty() || valueText.startsWith("\${") || valueText.startsWith("#{")) return null
        val module = ModuleUtilCore.findModuleForPsiElement(sourcePsi) ?: return null

        val findProperties = DefinedConfigurationPropertiesSearch.getInstance(sourcePsi.project)
            .findProperties(module, valueText)
        val property = findProperties.find { it.key == valueText } ?: return null

        val fixes = getReplacementStringQuickFix(property, sourcePsi, valueText)
        return manager.createProblemDescriptor(
            sourcePsi, SpringCoreBundle.message("explyt.spring.inspection.value.prefix"),
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