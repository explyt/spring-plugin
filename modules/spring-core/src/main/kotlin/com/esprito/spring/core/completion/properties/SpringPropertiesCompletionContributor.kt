package com.esprito.spring.core.completion.properties

import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

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
            if (propertyPosition.parent !is IProperty || !SpringCoreUtil.isConfigurationPropertyFile(parameters.originalFile)
            ) {
                return
            }

            if (propertyPosition is PropertyKeyImpl) {
                completePropertyKey(propertyPosition, parameters, result)
            }
            // completion of key value you can find in SpringConfigurationPropertiesValueReferenceProvider
        }

        private fun completePropertyKey(
            position: PropertyKeyImpl,
            parameters: CompletionParameters,
            result: CompletionResultSet
        ) {
            val module = ModuleUtilCore.findModuleForPsiElement(position) ?: return

            val cursor = parameters.offset
            val keyStart = position.textOffset

            val key = position.text.substring(0, cursor - keyStart)
            val fullKey = parameters.originalFile.text.substring(keyStart).substringBefore("\n").substringBefore("=")

            val properties = SpringConfigurationPropertiesSearch.getInstance(module.project)
                .getAllProperties(module)
            val parameterKeys = getParametersFromFile(parameters).map { it.key }.filter { it != fullKey }

            val reducedProperties = properties
                .groupBy { it.name }
                .map {
                    it.value
                        .reduce { first, second -> if (first.description != null || first.defaultValue != null) first else second }
                }
                .filter { it.name !in parameterKeys }

            for (property in reducedProperties) {
                result.withPrefixMatcher(key).addElement(
                    LookupElementBuilder.create(property, property.name)
                        .withRenderer(PropertyRenderer())
                )
            }
        }

        private fun getParametersFromFile(completionParameters: CompletionParameters): List<IProperty> {
            val psiFile = completionParameters.originalFile
            if (!SpringCoreUtil.isConfigurationPropertyFile(psiFile)) {
                return emptyList()
            }
            return (psiFile as PropertiesFile).properties.filterNotNull()
        }
    }
}
