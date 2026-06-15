/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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