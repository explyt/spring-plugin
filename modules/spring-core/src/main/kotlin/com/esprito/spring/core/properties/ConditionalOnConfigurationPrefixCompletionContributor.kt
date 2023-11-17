package com.esprito.spring.core.properties

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiJavaToken
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext

class ConditionalOnConfigurationPrefixCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(PsiJavaToken::class.java),
            ConditionalOnConfigurationPrefixCompletionProvider()
        )
    }

    class ConditionalOnConfigurationPrefixCompletionProvider : CompletionProvider<CompletionParameters>() {

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val psiLiteral = parameters.position as? PsiJavaToken ?: return
            val psiNameValue = psiLiteral.parentOfType<PsiNameValuePair>() ?: return
            val module = ModuleUtilCore.findModuleForPsiElement(psiNameValue) ?: return
            val psiAnnotationQn = psiNameValue.parentOfType<PsiAnnotation>()?.qualifiedName ?: return
            val attributeName = psiNameValue.name ?: return

            val annotationHolder = MetaAnnotationsHolder.of(module, SpringCoreClasses.CONDITIONAL_ON_PROPERTY)
            if (!annotationHolder.isAttributeRelatedWith(
                    psiAnnotationQn,
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