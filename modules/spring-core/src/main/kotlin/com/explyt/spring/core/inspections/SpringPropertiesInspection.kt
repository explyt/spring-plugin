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
import com.explyt.spring.core.completion.properties.PropertiesPropertySource
import com.explyt.spring.core.inspections.quickfix.ReplacementKeyQuickFix
import com.explyt.spring.core.util.PropertyUtil.toKebabCase
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.properties.PropertiesQuickFixFactory
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.Property
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class SpringPropertiesInspection : SpringBasePropertyInspection() {

    override fun loadFileProperties(file: PsiFile): List<DefinedConfigurationProperty> {
        return (file as? PropertiesFile)?.let { PropertiesPropertySource(it).properties } ?: emptyList()
    }

    override fun getKeyPsiElement(property: DefinedConfigurationProperty): PsiElement? {
        return (property.psiElement as? Property)?.firstChild
    }

    override fun getRemoveKeyQuickFixes(property: DefinedConfigurationProperty): List<LocalQuickFix> {
        return (property.psiElement as? Property)
            ?.let { PropertiesQuickFixFactory.getInstance().createRemovePropertyLocalFix(it) }
            ?.let { listOf(it) } ?: emptyList()
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
        ReplacementKeyQuickFix(toKebabCase(key), psiKey.parent)
    )

}