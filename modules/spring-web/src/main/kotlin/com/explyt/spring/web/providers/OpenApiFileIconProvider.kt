/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.web.SpringWebIcons
import com.explyt.spring.web.model.OpenApiSpecificationFinder
import com.explyt.spring.web.model.OpenApiSpecificationType
import com.intellij.ide.IconProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.Icon

class OpenApiFileIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        if (element !is PsiFile) return null

        val virtualFile = element.virtualFile ?: return null
        val specificationType = OpenApiSpecificationFinder.identifySpecificationType(virtualFile, element)
        val extension = virtualFile.extension ?: return null

        return if (specificationType is OpenApiSpecificationType.OpenApiUndefined) null
        else getIconForExtension(extension)
    }

    private fun getIconForExtension(extension: String): Icon? {
        if (extension.isBlank()) return null
        if (extension == "yaml" || extension == "yml") {
            return SpringWebIcons.OpenApiYaml
        } else if (extension == "json") {
            return SpringWebIcons.OpenApiJson
        }
        return null
    }
}