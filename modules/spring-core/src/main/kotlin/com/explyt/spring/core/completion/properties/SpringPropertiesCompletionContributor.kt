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

package com.explyt.spring.core.completion.properties

import com.explyt.spring.core.completion.renderer.PropertyRenderer
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticInsertHandler
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

class SpringPropertiesCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), SpringPropertyCompletionProvider())
    }

    class SpringPropertyCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val propertyPosition = parameters.position
            val psiElement = propertyPosition.parent
            if (!SpringCoreUtil.isConfigurationPropertyFile(parameters.originalFile)) {
                return
            }

            when (psiElement) {
                is IProperty -> completeKey(propertyPosition, parameters, result)
                is YAMLKeyValue -> completeKey(psiElement, parameters, result, isYaml = true)
            }
        }

        private fun completeKey(
            element: PsiElement,
            parameters: CompletionParameters,
            result: CompletionResultSet,
            isYaml: Boolean = false
        ) {
            val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
            val currentKey = getCurrentKey(element, parameters) ?: return

            val properties = SpringConfigurationPropertiesSearch.getInstance(module.project)
                .getAllPropertiesWithSubKeys(module)
            val parameterKeys = getKeysFromFile(parameters).filter { it != currentKey.fullKey }

            val reducedProperties = properties
                .groupBy { it.name }
                .map {
                    it.value
                        .reduce { first, second -> if (first.description != null || first.defaultValue != null) first else second }
                }
                .filter { it.name !in parameterKeys }

            for (property in reducedProperties) {
                val shouldIncludeProperty = !isYaml ||
                        property.name.startsWith(currentKey.prefix) ||
                        (currentKey.prefix.isEmpty() && currentKey.startKey.isEmpty())

                if (shouldIncludeProperty) {
                    val lookupElement = LookupElementBuilder.create(property, property.name)
                        .withRenderer(PropertyRenderer())
                        .withInsertHandler { insertionContext, _ ->
                            if (isYaml) {
                                handleInsertInYaml(currentKey, property, insertionContext)
                            } else {
                                StatisticInsertHandler(StatisticActionId.COMPLETION_PROPERTY_KEY_CONFIGURATION)
                            }
                        }

                    result.withPrefixMatcher(currentKey.startKey).addElement(lookupElement)
                }
            }
        }

        private fun handleInsertInYaml(
            currentKey: CurrentKey,
            property: ConfigurationProperty,
            insertionContext: InsertionContext
        ) {
            val insertName =
                if (currentKey.prefix.isEmpty())
                    property.name
                else if (property.name.startsWith("${currentKey.prefix}.")) {
                    property.name.substringAfter("${currentKey.prefix}.")
                } else ""
            handleInsert(insertionContext, insertName)
            StatisticService.getInstance().addActionUsage(StatisticActionId.COMPLETION_YAML_KEY_CONFIGURATION)
        }

        private fun handleInsert(context: InsertionContext, text: String) {
            val startOffset = context.startOffset
            val tailOffset = context.tailOffset

            context.editor.document.replaceString(startOffset, tailOffset, text)
            context.editor.caretModel.moveToOffset(startOffset + text.length)
            context.commitDocument()
        }

        private fun getCurrentKey(element: PsiElement, parameters: CompletionParameters): CurrentKey? {
            return when (element) {
                is PropertyKeyImpl -> getCurrentKeyFromProperty(element, parameters)
                is YAMLKeyValue -> getCurrentKeyFromYaml(element, parameters)
                else -> null
            }
        }

        private fun getCurrentKeyFromProperty(
            propertyKey: PropertyKeyImpl,
            parameters: CompletionParameters
        ): CurrentKey {
            val cursor = parameters.offset
            val keyStart = propertyKey.textOffset

            val key = propertyKey.text.substring(0, cursor - keyStart)
            val prefix = key.substringBeforeLast(".")
            val fullKey = parameters.originalFile.text
                .substring(keyStart)
                .substringBefore("\n").substringBefore("=")
            return CurrentKey(prefix, key, fullKey)
        }

        private fun getCurrentKeyFromYaml(yamlKey: YAMLKeyValue, parameters: CompletionParameters): CurrentKey {
            val cursor = parameters.offset
            val keyStart = parameters.position.textOffset
            val key = parameters.position.text.substring(0, cursor - keyStart)

            val keyResult = YAMLUtil.getConfigFullName(yamlKey)
            val prefix = if (key.isEmpty()) keyResult.substringBefore(".${CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED}")
            else keyResult.substringBefore(".$key")
            val fullKey = keyResult
                .replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "").replace(" ", "")
            return CurrentKey(prefix, key, fullKey)
        }

        private fun getKeysFromFile(completionParameters: CompletionParameters): List<String> {
            val psiFile = completionParameters.originalFile
            if (!SpringCoreUtil.isConfigurationPropertyFile(psiFile)) {
                return emptyList()
            }
            val keys = mutableListOf<String>()
            if (psiFile is PropertiesFile) {
                keys.addAll(psiFile.properties.mapNotNull { it.key })
            }
            if (psiFile is YAMLFile) {
                keys.addAll(YamlPropertySource(psiFile).properties.map { it.key })
            }
            return keys
        }
    }

    data class CurrentKey(
        val prefix: String,
        val startKey: String,
        val fullKey: String
    )
}
