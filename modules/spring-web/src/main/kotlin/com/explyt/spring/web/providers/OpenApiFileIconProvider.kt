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