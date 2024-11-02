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

package com.explyt.spring.core.intentions

import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence

class CreateYamlMetaDescriptionIntention : BaseCreateMetaDescriptionIntention() {

    override fun isAvailable(psiElement: PsiElement): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return false

        val parent = psiElement.parent as? YAMLKeyValue ?: return false
        if (parent.key !== psiElement) return false

        val value = parent.value ?: return false
        if (value !is YAMLScalar && value !is YAMLSequence) return false

        val keyName = YAMLUtil.getConfigFullName(parent)

        return SpringConfigurationPropertiesSearch.getInstance(psiElement.project)
            .findProperty(module, keyName) == null
    }

    override fun isAvailable(file: PsiFile): Boolean {
        if (!SpringCoreUtil.isConfigurationPropertyFile(file)) return false
        return YAMLLanguage.INSTANCE == file.language
    }

    override fun rootArrayName(): String = "properties"

    override fun getPropertyInfo(editor: Editor, file: PsiFile): PropertyInfo? {
        val parentProperty = findElementAtCaret(editor, file)?.parent as? YAMLKeyValue ?: return null
        val keyName = YAMLUtil.getConfigFullName(parentProperty)
        val type = PropertyUtil.guessTypeFromValue(parentProperty.valueText)

        return PropertyInfo(
            name = keyName,
            type = type,
            parentProperty
        )
    }


}