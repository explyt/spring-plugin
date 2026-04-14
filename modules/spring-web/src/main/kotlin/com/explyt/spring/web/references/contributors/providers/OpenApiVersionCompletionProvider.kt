/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.references.contributors.providers

import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.util.OpenApiFileUtil
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

class OpenApiVersionCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val psiFile = parameters.originalFile
        val virtualFile = psiFile.virtualFile ?: return

        if (!OpenApiFileUtil.INSTANCE.isOpenApiFile(virtualFile, psiFile)) return

        if (psiFile.fileType is JsonFileType) {
            jsonAddCompletions(parameters.position, result)
        } else if (psiFile.fileType is YAMLFileType) {
            yamlAddCompletions(parameters.position, result)
        }
    }

    private fun jsonAddCompletions(element: PsiElement, result: CompletionResultSet) {
        val property = element.getParentOfType<JsonProperty>(true) ?: return

        val isJsonObject = property.parent is JsonObject
        val isJsonFile = property.parent.parent is JsonFile

        val keyText = property.name

        if (keyText == SpringWebUtil.OPEN_API && isJsonObject && isJsonFile && property.value?.text == element.text) {
            addLookup(result, true)
        }

    }

    private fun yamlAddCompletions(element: PsiElement, result: CompletionResultSet) {
        val isTypeDString = element.node.elementType == YAMLTokenTypes.SCALAR_DSTRING
        val isTypeText = element.node.elementType == YAMLTokenTypes.TEXT

        val parent = element.parent.parent as? YAMLKeyValue ?: return

        if (parent.keyText == SpringWebUtil.OPEN_API && parent.parent is YAMLMapping && (isTypeDString || isTypeText)) {
            addLookup(result)
        }
    }

    private fun addLookup(result: CompletionResultSet, quotes: Boolean = false) {
        val openApiVersions = listOf(
            "3.0.0" to SpringWebBundle.message("explyt.openapi.3.0.schema.name"),
            "3.1.0" to SpringWebBundle.message("explyt.openapi.3.1.schema.name")
        )

        openApiVersions.forEach { (version, message) ->
            val lookupString = if (quotes) "\"$version\"" else version
            result.addElement(
                LookupElementBuilder.create(lookupString)
                    .withTypeText(message)
            )
        }
    }
}