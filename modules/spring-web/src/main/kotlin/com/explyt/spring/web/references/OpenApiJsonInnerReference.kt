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
import com.intellij.json.JsonUtil
import com.intellij.json.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.utils.addToStdlib.lastIndexOfOrNull

class OpenApiJsonInnerReference(element: PsiElement) : PsiReferenceBase<PsiElement>(element), HighlightedReference {

    override fun resolve(): PsiElement? {
        val literal = jsonLiteral() ?: return null
        val jsonFile = literal.containingFile as? JsonFile ?: return null

        val keyToFind = literal.value //value example: #/some/element/PathInsideFile
            .substring(2)
        if (keyToFind.isEmpty()) return null

        val path = keyToFind.split('/').toMutableList()

        return jsonFile.getProperty(path)?.navigationElement
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val originalValue = jsonLiteral()?.value ?: return super.handleElementRename(newElementName)
        val lastElementPos = originalValue.lastIndexOfOrNull('/') ?: return super.handleElementRename(newElementName)
        val prefix = originalValue.substring(0, lastElementPos)

        return super.handleElementRename("$prefix/$newElementName")
    }

    private fun jsonLiteral(): JsonStringLiteral? =
        element as? JsonStringLiteral

    companion object {
        fun JsonFile.getProperty(propertyFqn: Collection<String>): JsonProperty? {
            val path = propertyFqn.toMutableList()
            var jsonNode: JsonElement? = JsonUtil.getTopLevelObject(this) ?: return null

            while (path.isNotEmpty()) {
                val pathElement = path.removeFirst()
                jsonNode = if (jsonNode is JsonObject) jsonNode else jsonNode?.getChildOfType<JsonObject>()

                jsonNode = jsonNode?.childrenOfType<JsonProperty>()
                    ?.firstOrNull { it.name == pathElement } ?: return null
            }

            return jsonNode as? JsonProperty
        }
    }

}