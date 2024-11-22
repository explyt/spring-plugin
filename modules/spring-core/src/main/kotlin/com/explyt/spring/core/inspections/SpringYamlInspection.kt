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

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.completion.properties.YamlPropertySource
import com.explyt.spring.core.inspections.quickfix.YamlKeyToKebabQuickFix
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.project.Project
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.YAMLBundle
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

class SpringYamlInspection : SpringBasePropertyInspection() {

    override fun loadFileProperties(file: PsiFile): List<DefinedConfigurationProperty> {
        return (file as? YAMLFile)?.let { YamlPropertySource(it).properties } ?: emptyList()
    }

    override fun getKeyPsiElement(property: DefinedConfigurationProperty): PsiElement? {
        return (property.psiElement as? YAMLKeyValue)?.key
    }

    override fun getRemoveKeyQuickFixes(property: DefinedConfigurationProperty): List<LocalQuickFix> {
        return listOf(RemoveDuplicatedKeyQuickFix())
    }

    override fun keyShouldBeKebabProblemDescriptor(
        manager: InspectionManager,
        psiKey: PsiElement,
        isOnTheFly: Boolean,
        key: String
    ): ProblemDescriptor = manager.createProblemDescriptor(
        psiKey,
        ElementManipulators.getValueTextRange(psiKey),
        SpringCoreBundle.message("explyt.spring.inspection.properties.value.should.be.kebab"),
        ProblemHighlightType.WARNING,
        isOnTheFly,
        YamlKeyToKebabQuickFix(psiKey.parent)
    )
}

private class RemoveDuplicatedKeyQuickFix : LocalQuickFix {
    override fun getFamilyName(): String {
        return YAMLBundle.message("YAMLDuplicatedKeysInspection.remove.key.quickfix.name")
    }

    override fun availableInBatchMode() = false

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val keyVal = descriptor.psiElement.parent as? YAMLKeyValue ?: return
        keyVal.parentMapping?.deleteKeyValue(keyVal)
    }
}