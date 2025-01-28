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

package com.explyt.spring.core.properties.rename

import com.explyt.spring.core.util.RenameUtil
import com.intellij.lang.properties.IProperty
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue


class SpringValueRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is YAMLKeyValue || element is IProperty
    }

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
        when (element) {
            is YAMLKeyValue -> {
                val fullName = YAMLUtil.getConfigFullName(element)
                val newFullName = fullName.replace(element.keyText, newName)
                RenameUtil.renameSameProperty(element.project, element, fullName, newFullName)
            }

            is IProperty -> {
                val key = element.key
                if (key != null) {
                    RenameUtil.renameSameProperty(element.project, element, key, newName)
                }
            }
        }
        super.prepareRenaming(element, newName, allRenames)
    }
}