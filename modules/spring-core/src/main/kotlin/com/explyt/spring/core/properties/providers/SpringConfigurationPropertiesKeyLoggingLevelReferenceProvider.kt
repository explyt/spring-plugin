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

package com.explyt.spring.core.properties.providers

import com.explyt.spring.core.SpringProperties.LOGGING_LEVEL
import com.explyt.spring.core.SpringProperties.PLACEHOLDER_PREFIX
import com.explyt.spring.core.SpringProperties.POSTFIX_KEYS
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.ValueHint
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticInsertHandler
import com.explyt.util.ExplytTextUtil.getFirstSentenceWithoutDot
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet
import com.intellij.util.ProcessingContext

class SpringConfigurationPropertiesKeyLoggingLevelReferenceProvider : PsiReferenceProvider() {

    private val referenceProvider = JavaClassReferenceProvider()
        .apply { isSoft = true }

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val valueTextRange = ElementManipulators.getValueTextRange(element)
        val startInElement = LOGGING_LEVEL.length + 1

        if (startInElement > valueTextRange.length) return PsiReference.EMPTY_ARRAY

        val textRange = valueTextRange.shiftRight(startInElement).grown(-startInElement)

        val elementText = element.text
        val rangeText = textRange.substring(elementText)
        if (rangeText.contains(PLACEHOLDER_PREFIX)) return PsiReference.EMPTY_ARRAY

        val offset = textRange.startOffset
        val classReferences = JavaClassReferenceSet(rangeText, element, offset, false, referenceProvider)
            .references

        val valuesReference = buildReferencesFromValueHints(element, offset)

        return arrayOf(*valuesReference, *classReferences)
    }

    private fun buildReferencesFromValueHints(
        element: PsiElement,
        textOffset: Int
    ): Array<PsiReference> {

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return PsiReference.EMPTY_ARRAY
        val hint = SpringConfigurationPropertiesSearch.getInstance(element.project)
            .findHint(module, LOGGING_LEVEL + POSTFIX_KEYS) ?: return PsiReference.EMPTY_ARRAY
        return hint.values.mapNotNull { valueHint ->

            val value = valueHint.value ?: return@mapNotNull null
            val startOffset = textOffset
            val endOffset = textOffset + value.length

            val textRange = TextRange(startOffset, endOffset)

            LoggingLevelKeysPsiReference(element, textRange, valueHint)
        }.toTypedArray()
    }

}

class LoggingLevelKeysPsiReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val valueHint: ValueHint
) : PsiReferenceBase.Poly<PsiElement>(element, rangeInElement, true) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return emptyArray()
    }

    override fun getVariants(): Array<LookupElementBuilder> {
        val lookupString = valueHint.value ?: return emptyArray()
        val tailText = if (valueHint.description != null)
            " (${getFirstSentenceWithoutDot(valueHint.description)})" else ""
        return arrayOf(
            LookupElementBuilder.create(lookupString)
                .appendTailText(tailText, true)
                .withIcon(AllIcons.Nodes.Property)
                .withInsertHandler(StatisticInsertHandler(StatisticActionId.COMPLETION_PROPERTY_KEY_CONFIGURATION))
        )
    }

}
