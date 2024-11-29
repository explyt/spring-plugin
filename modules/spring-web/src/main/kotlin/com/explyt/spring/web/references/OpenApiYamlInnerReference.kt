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

package com.explyt.spring.web.references

import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.kotlin.utils.addToStdlib.lastIndexOfOrNull
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLScalar

class OpenApiYamlInnerReference(element: PsiElement) : PsiReferenceBase<PsiElement>(element), HighlightedReference {

    override fun resolve(): PsiElement? {
        val yamlScalar = yamlScalar() ?: return null
        val yamlFile = yamlScalar.containingFile as? YAMLFile ?: return null

        val keyToFind = yamlScalar.textValue //value example: #/some/element/PathInsideFile
            .substring(2)

        return YAMLUtil.getQualifiedKeyInFile(
            yamlFile,
            keyToFind.split('/')
        )?.navigationElement
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val originalValue = yamlScalar()?.textValue ?: return super.handleElementRename(newElementName)
        val lastElementPos = originalValue.lastIndexOfOrNull('/') ?: return super.handleElementRename(newElementName)
        val prefix = originalValue.substring(0, lastElementPos)

        return super.handleElementRename("$prefix/$newElementName")
    }

    private fun yamlScalar(): YAMLScalar? =
        element as? YAMLScalar

}