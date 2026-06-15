/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.rename

import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.RenameUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.toUElement

class ConfigurationPropertyPsiElementProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return if (element.language == KotlinLanguage.INSTANCE) {
            return ((element.toUElement() as? UParameter)?.uastParent as? UMethod)?.isConstructor == true
        } else element is PsiMethod && element.name.startsWith("set")
    }

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
        val propertyName = newName.substringAfter("set").takeIf { it.isNotBlank() } ?: return
        val newPropertyName = RenameUtil.convertSetterToPKebabCase(propertyName)

        val propertyResult = when {
            element.language == KotlinLanguage.INSTANCE -> {
                (element.toUElement() as? UParameter)
                    ?.takeIf { (it.uastParent as? UMethod)?.isConstructor == true }
                    ?.let { PropertyUtil.findPropertyByConfigurationPropertyElement(element) }
            }

            element is PsiMethod -> PropertyUtil.findPropertyByConfigurationPropertyElement(element)
            else -> null
        } ?: return

        val prefix = RenameUtil.convertSetterToPKebabCase(propertyResult.prefix)
        propertyResult.properties.forEach {
            RenameUtil.renameProperty(it, newPropertyName, prefix, true)
        }

        super.prepareRenaming(element, newName, allRenames)
    }
}