/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.rename

import com.explyt.spring.core.util.RenameUtil
import com.intellij.lang.properties.IProperty
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.yaml.psi.YAMLKeyValue


class SpringValueRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is YAMLKeyValue || element is IProperty
    }

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
        RenameUtil.renameProperty(element, newName)
        super.prepareRenaming(element, newName, allRenames)
    }
}