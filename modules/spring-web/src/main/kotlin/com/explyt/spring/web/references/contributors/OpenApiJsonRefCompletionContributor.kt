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

package com.explyt.spring.web.references.contributors

import com.explyt.spring.core.statistic.StatisticActionId.COMPLETION_OPENAPI_JSON_ENDPOINT
import com.explyt.spring.core.statistic.StatisticInsertHandler
import com.explyt.spring.web.references.OpenApiJsonInnerReference.Companion.getProperty
import com.explyt.spring.web.references.contributors.providers.OpenApiVersionCompletionProvider
import com.explyt.spring.web.util.PlatformPatternUtils
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class OpenApiJsonRefCompletionContributor : CompletionContributor() {

    init {
        extend(
            null,
            psiElement()
                .withParent(
                    PlatformPatternUtils.openApiJsonInnerRef()
                ),
            OpenApiJsonRefCompletionProvider()
        )

        extend(CompletionType.BASIC, psiElement(), OpenApiVersionCompletionProvider())
    }


    class OpenApiJsonRefCompletionProvider : CompletionProvider<CompletionParameters>() {

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val literal = parameters.position.parent as? JsonStringLiteral ?: return
            val jsonFile = literal.containingFile as? JsonFile ?: return

            for (component in componentsToScan) {
                val jsonPropertyValue = jsonFile
                    .getProperty(component.split('/'))
                    ?.getChildOfType<JsonObject>() ?: continue

                val prefix = "#/$component/"
                result.addAllElements(
                    jsonPropertyValue.childrenOfType<JsonProperty>()
                        .map {
                            LookupElementBuilder
                                .create("$prefix${it.name}")
                                .withInsertHandler(StatisticInsertHandler(COMPLETION_OPENAPI_JSON_ENDPOINT))
                        }
                )
            }
        }

        companion object {
            private val componentsToScan = setOf("components/parameters", "components/responses", "components/schemas")
        }

    }

}