/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.getUnquotedText
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.json.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.childrenOfType

class JsonRunEndpointLineMarkerProvider : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (element !is LeafPsiElement) return null
        if (!SpringWebUtil.isSpringWebProject(element.project)) return null
        val jsonProperty = PsiTreeUtil.getParentOfType(
            element,
            JsonStringLiteral::class.java,
            false
        )?.parent as? JsonProperty ?: return null
        val elementText = getUnquotedText(element)
        if (elementText != jsonProperty.name) return null
        val jsonFile = jsonProperty.containingFile as? JsonFile ?: return null
        if (!OpenApiUtils.isOpenApi(jsonFile)) return null

        if (jsonProperty.name !in SpringWebUtil.REQUEST_METHODS) return null

        val tags = getSubPropertyValue(jsonProperty, "tags") ?: return null
        if (tags !is JsonArray) return null
        val firstTag = tags.valueList.firstOrNull() ?: return null
        val operationId = getSubPropertyValue(jsonProperty, "operationId") ?: return null

        return Info(
            OpenApiUtils.createPreviewAction(getUnquotedText(firstTag), getUnquotedText(operationId))
        )
    }

    private fun getSubPropertyValue(jsonProperty: JsonProperty, key: String): JsonValue? {
        return jsonProperty.value
            ?.childrenOfType<JsonProperty>()?.asSequence()
            ?.filter { it.name == key }
            ?.mapNotNull { it.value }
            ?.firstOrNull()
    }

}