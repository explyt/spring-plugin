package com.esprito.spring.core.properties.contributors

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UNamedExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

class ConditionalOnConfigurationPrefixCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            ConditionalOnConfigurationPrefixCompletionProvider()
        )
    }

    class ConditionalOnConfigurationPrefixCompletionProvider : CompletionProvider<CompletionParameters>() {

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val psiElement = parameters.position
            val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return
            val uElement = psiElement.parent.toUElement() ?: return
            val uLiteralExpression = uElement.getParentOfType<UNamedExpression>() ?: return
            val uAnnotation = uElement.getParentOfType<UAnnotation>() ?: return
            val annotationQn = uAnnotation.qualifiedName ?: return
            val attributeName = uLiteralExpression.name ?: return

            val annotationHolder = MetaAnnotationsHolder.of(module, SpringCoreClasses.CONDITIONAL_ON_PROPERTY)
            if (!annotationHolder.isAttributeRelatedWith(
                    annotationQn,
                    attributeName,
                    SpringCoreClasses.CONDITIONAL_ON_PROPERTY,
                    setOf("prefix")
                )
            ) return

            val allProperties = SpringConfigurationPropertiesSearch.getInstance(module.project)
                .getAllProperties(module)
            for (property in allProperties) {
                sequenceOf(property.name)
                    .map { getFullPrefix(it) }
                    .filter { it.isNotBlank() }
                    .flatMap { getSubPrefixes(it).asSequence() }
                    .sorted()
                    .map {
                        LookupElementBuilder.create(it)
                            .withIcon(AllIcons.Nodes.Property)
                            .withTypeText(getTypeText(property.sourceType))
                    }
                    .forEach { result.addElement(it) }
            }
            result.stopHere()
        }

        private fun getFullPrefix(property: String): String {
            val lastDotPos = property.indexOfLast { it == SEPARATOR }
            if (lastDotPos == -1) return ""

            return property.substring(0, lastDotPos)
        }

        private fun getSubPrefixes(fullPrefix: String): List<String> {
            val result = mutableListOf<String>()

            var dotPos = fullPrefix.indexOf(SEPARATOR)
            while (dotPos != -1) {
                result += fullPrefix.substring(0, dotPos)
                dotPos = fullPrefix.indexOf(SEPARATOR, dotPos + 1)
            }
            result += fullPrefix

            return result
        }

        private fun getTypeText(sourceType: String?): String? {
            return sourceType
                ?.substringAfterLast(SEPARATOR)
                ?.substringBefore('#')
                ?.substringBefore('$')
        }

        companion object {
            const val SEPARATOR = '.'
        }

    }

}