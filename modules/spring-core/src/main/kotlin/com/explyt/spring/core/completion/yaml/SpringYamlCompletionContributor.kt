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

package com.explyt.spring.core.completion.yaml

import com.explyt.spring.core.SpringProperties.LOGGING_LEVEL
import com.explyt.spring.core.completion.insertHandler.YamlKeyConfigurationPropertyInsertHandler
import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.completion.renderer.PropertyRenderer
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.core.util.YamlUtil
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiComment
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLPsiElement
import org.jetbrains.yaml.psi.YAMLSequence


class SpringYamlCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), SpringYamlCompletionProvider())
    }

    class SpringYamlCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet,
        ) {
            val positionElement = parameters.position
            if (positionElement is PsiComment) return
            val parentElement = positionElement.parent as? YAMLPsiElement ?: return
            val elementContext = positionElement.context ?: return
            if (elementContext.parent is YAMLSequence
                || !SpringCoreUtil.isConfigurationPropertyFile(parameters.originalFile)
            ) {
                return
            }

            val module = ModuleUtilCore.findModuleForPsiElement(positionElement) ?: return

            val document = parentElement.parentOfType<YAMLDocument>() ?: return
            val existProperties = YamlUtil.getAllProperties(document)

            val cursor = parameters.offset
            val keyStart = positionElement.textOffset
            val positionText = positionElement.text.substring(0, cursor - keyStart)
            val configFullName = YAMLUtil.getConfigFullName(parentElement)

            val properties = SpringConfigurationPropertiesSearch.getInstance(module.project)
                .getAllPropertiesWithSubKeys(module)
            val matchingProperties = properties.filter {
                it.name !in existProperties && (configFullName.isEmpty() || it.name.startsWith("$configFullName."))
            }
            matchingProperties.forEach {
                val property = it.copy(inLineYaml = positionText.contains("."))
                result.withPrefixMatcher(positionText).addElement(createLookupElement(property, configFullName))
            }
            if (matchingProperties.any { !it.isMap() && !it.name.startsWith("$LOGGING_LEVEL.") }) {
                result.stopHere()
            }
        }

        private fun createLookupElement(
            configurationProperty: ConfigurationProperty,
            configFullName: String
        ): LookupElementBuilder {
            val propertyName = configurationProperty.name
            val name = if (configFullName.isEmpty()) propertyName else propertyName.substringAfter(
                "$configFullName."
            )
            return LookupElementBuilder.create(configurationProperty, name)
                .withInsertHandler(YamlKeyConfigurationPropertyInsertHandler())
                .withRenderer(PropertyRenderer())
        }
    }
}
