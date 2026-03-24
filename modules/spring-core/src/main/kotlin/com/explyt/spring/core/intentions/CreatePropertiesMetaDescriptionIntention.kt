/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.intentions

import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.PropertyUtil.propertyKey
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.lang.properties.PropertiesLanguage
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class CreatePropertiesMetaDescriptionIntention : BaseCreateMetaDescriptionIntention() {

    override fun isAvailable(psiElement: PsiElement): Boolean {
        val propertyKey = psiElement as? PropertyKeyImpl ?: return false
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return false
        val keyName = propertyKey.propertyKey() ?: return false

        return SpringConfigurationPropertiesSearch.getInstance(psiElement.project)
            .findProperty(module, keyName) == null
    }

    override fun isAvailable(file: PsiFile): Boolean {
        if (!SpringCoreUtil.isConfigurationPropertyFile(file)) return false
        return PropertiesLanguage.INSTANCE == file.language
    }

    override fun rootArrayName(): String = "properties"

    override fun getPropertyInfo(editor: Editor, file: PsiFile): PropertyInfo? {
        val parentProperty = findElementAtCaret(editor, file)?.parent as? PropertyImpl ?: return null
        val keyName = parentProperty.name ?: return null
        val type = PropertyUtil.guessTypeFromValue(parentProperty.value)

        return PropertyInfo(
            name = keyName,
            type = type,
            parentProperty
        )
    }

}