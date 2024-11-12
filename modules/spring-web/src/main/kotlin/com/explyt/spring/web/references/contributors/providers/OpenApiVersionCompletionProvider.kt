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

package com.explyt.spring.web.references.contributors.providers

import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.util.OpenApiFileHelper
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

        if (!OpenApiFileHelper.INSTANCE.isSuitableFile(virtualFile, psiFile)) return

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