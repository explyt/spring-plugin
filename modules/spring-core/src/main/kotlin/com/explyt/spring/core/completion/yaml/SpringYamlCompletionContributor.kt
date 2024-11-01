package com.explyt.spring.core.completion.yaml

import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.explyt.spring.core.completion.properties.PropertyRenderer
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
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
            properties.filter {
                it.name !in existProperties && (configFullName.isEmpty() || it.name.startsWith("$configFullName."))
            }.forEach {
                val property = it.copy(inLineYaml = positionText.contains("."))
                result.withPrefixMatcher(positionText).addElement(createLookupElement(property, configFullName))
            }
            result.stopHere()
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
