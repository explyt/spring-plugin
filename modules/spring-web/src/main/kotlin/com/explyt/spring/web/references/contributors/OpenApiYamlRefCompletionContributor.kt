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

import com.explyt.spring.core.statistic.StatisticActionId.COMPLETION_OPENAPI_YAML_ENDPOINT
import com.explyt.spring.core.statistic.StatisticInsertHandler
import com.explyt.spring.web.references.contributors.providers.OpenApiVersionCompletionProvider
import com.explyt.spring.web.util.PlatformPatternUtils
import com.explyt.util.ExplytKotlinUtil.mapToList
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar

class OpenApiYamlRefCompletionContributor : CompletionContributor() {

    init {
        extend(
            null,
            psiElement()
                .withParent(
                    PlatformPatternUtils.openApiYamlInnerRef()
                ),
            OpenApiYamlRefCompletionProvider()
        )

        extend(CompletionType.BASIC, psiElement(), OpenApiVersionCompletionProvider())
    }


    class OpenApiYamlRefCompletionProvider : CompletionProvider<CompletionParameters>() {

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val yamlScalar = parameters.position.parent as? YAMLScalar ?: return
            val yamlFile = yamlScalar.containingFile as? YAMLFile ?: return

            val componentsKey = YAMLUtil.getQualifiedKeyInFile(yamlFile, KEY_COMPONENTS) ?: return
            val componentsKeyValues = componentsKey.childrenOfType<YAMLMapping>().asSequence()
                .flatMap { it.keyValues }
                .filter { it.keyText in componentsToScan }

            for (componentKeyValue in componentsKeyValues) {
                val prefix = "#/$KEY_COMPONENTS/${componentKeyValue.keyText}/"

                result.addAllElements(
                    componentKeyValue.childrenOfType<YAMLMapping>().asSequence()
                        .flatMap { it.keyValues }
                        .mapToList {
                            LookupElementBuilder
                                .create("$prefix${it.keyText}")
                                .withInsertHandler(StatisticInsertHandler(COMPLETION_OPENAPI_YAML_ENDPOINT))
                        }
                )
            }
        }

        companion object {
            const val KEY_COMPONENTS = "components"
            private val componentsToScan = setOf("parameters", "responses", "schemas")
        }

    }

}