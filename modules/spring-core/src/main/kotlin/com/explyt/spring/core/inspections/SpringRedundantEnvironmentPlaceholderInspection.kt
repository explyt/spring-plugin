/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringProperties.SPRING_PROFILES_ACTIVE
import com.explyt.spring.core.SpringProperties.SPRING_PROFILES_GROUP
import com.explyt.spring.core.completion.properties.PropertiesPropertySource
import com.explyt.spring.core.completion.properties.YamlPropertySource
import com.explyt.spring.core.inspections.quickfix.ReplaceEnvironmentPlaceholderWithDefaultQuickFix
import com.explyt.spring.core.util.ConfigurationPlaceholder
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.PropertyUtil.propertyValuePsiElement
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLFile

class SpringRedundantEnvironmentPlaceholderInspection : SpringBaseLocalInspectionTool() {
    override fun getDefaultLevel(): HighlightDisplayLevel = HighlightDisplayLevel.WEAK_WARNING

    override fun isAvailableForFile(file: PsiFile): Boolean =
        (file is PropertiesFile || file is YAMLFile) &&
            super.isAvailableForFile(file) && SpringCoreUtil.isConfigurationPropertyFile(file)

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<ProblemDescriptor> {
        if (!SpringCoreUtil.isConfigurationPropertyFile(file)) return ProblemDescriptor.EMPTY_ARRAY
        val properties = when (file) {
            is PropertiesFile -> PropertiesPropertySource(file).properties
            is YAMLFile -> YamlPropertySource(file).properties
            else -> return ProblemDescriptor.EMPTY_ARRAY
        }
        return properties.flatMap { property ->
            ProgressManager.checkCanceled()
            val key = property.key
            val bootstrapKey = PropertyUtil.toCommonPropertyForm(key).substringBefore('[')
            if (bootstrapKey in BOOTSTRAP_KEYS || bootstrapKey.startsWith("$SPRING_PROFILES_GROUP.")) {
                return@flatMap emptyList()
            }
            val element = property.psiElement?.propertyValuePsiElement() ?: return@flatMap emptyList()
            val valueRange = ElementManipulators.getValueTextRange(element)
            val placeholder = ConfigurationPlaceholder.parse(valueRange.substring(element.text))
                ?: return@flatMap emptyList()
            if (placeholder.name != PropertyUtil.toSystemEnvironmentForm(key)) return@flatMap emptyList()
            val fixes = if (placeholder.replacementDefault(element, valueRange) != null) {
                arrayOf<LocalQuickFix>(ReplaceEnvironmentPlaceholderWithDefaultQuickFix())
            } else emptyArray()
            placeholder.redundantRanges.mapIndexed { index, range ->
                manager.createProblemDescriptor(
                    element,
                    range.shiftRight(valueRange.startOffset),
                    SpringCoreBundle.message("explyt.spring.inspection.environment.placeholder.message"),
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    isOnTheFly,
                    *if (index == 0) fixes else emptyArray(),
                ).also { it.setTextAttributes(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES) }
            }
        }.toTypedArray()
    }

    private companion object {
        val BOOTSTRAP_KEYS = setOf(
            "spring.config.import",
            "spring.config.activate.on-profile",
            "spring.config.activate.on-cloud-platform",
            "spring.config.location",
            "spring.config.additional-location",
            "spring.config.name",
            SPRING_PROFILES_ACTIVE,
            "spring.profiles.include",
            SPRING_PROFILES_GROUP,
        ).mapTo(mutableSetOf(), PropertyUtil::toCommonPropertyForm)
    }
}
