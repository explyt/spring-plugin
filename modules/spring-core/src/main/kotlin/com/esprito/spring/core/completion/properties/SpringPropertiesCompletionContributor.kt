package com.esprito.spring.core.completion.properties

import com.esprito.spring.core.util.SpringCoreUtil
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

            if (psiElement is IProperty) {
                completeKey(propertyPosition, parameters, result)
            } else if (psiElement is YAMLKeyValue) {
                completeKey(psiElement, parameters, result)
            }
        }

        private fun completeKey(
            element: PsiElement,
            parameters: CompletionParameters,
            result: CompletionResultSet
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
                result.withPrefixMatcher(currentKey.startKey).addElement(
                    LookupElementBuilder.create(property, property.name)
                        .withRenderer(PropertyRenderer())
                )
            }
        }

        private fun getCurrentKey(element: PsiElement, parameters: CompletionParameters): CurrentKey? {
            return when (element) {
                is PropertyKeyImpl -> {
                    getCurrentKeyFromProperty(element, parameters)
                }

                is YAMLKeyValue -> {
                    getCurrentKeyFromYaml(element)
                }

                else -> {
                    null
                }
            }
        }

        private fun getCurrentKeyFromProperty(
            propertyKey: PropertyKeyImpl,
            parameters: CompletionParameters
        ): CurrentKey {
            val cursor = parameters.offset
            val keyStart = propertyKey.textOffset

            val key = propertyKey.text.substring(0, cursor - keyStart)
            val fullKey = parameters.originalFile.text
                .substring(keyStart)
                .substringBefore("\n").substringBefore("=")
            return CurrentKey(key, fullKey)
        }

        private fun getCurrentKeyFromYaml(yamlKey: YAMLKeyValue): CurrentKey {
            val keyResult = YAMLUtil.getConfigFullName(yamlKey)
            val key = keyResult.substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)
            val fullKey = keyResult.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "").replace(" ", "")
            return CurrentKey(key, fullKey)
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
        val startKey: String,
        val fullKey: String
    )
}
