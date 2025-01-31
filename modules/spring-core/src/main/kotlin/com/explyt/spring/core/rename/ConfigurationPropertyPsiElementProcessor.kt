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