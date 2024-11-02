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

package com.explyt.spring.core.search

import com.explyt.spring.core.properties.references.ExplytPropertyReference.Companion.PROPERTY_REFERENCE_ORIGINAL_TEXT
import com.explyt.spring.core.util.PropertyUtil
import com.intellij.lang.properties.IProperty
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

class PropertyStringUsagesSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {

    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        val searchTextHolder = runReadAction { getKeyName(queryParameters.elementToSearch) } ?: return
        val additionalWords = getAdditionalWordsToSearch(searchTextHolder).takeIf { it.isNotEmpty() } ?: return

        val effectiveSearchScope = queryParameters.effectiveSearchScope
        if (SearchScope.isEmptyScope(effectiveSearchScope)) return
        val searchContext = (UsageSearchContext.IN_CODE.toInt() or UsageSearchContext.IN_STRINGS.toInt()).toShort()

        for (word in additionalWords) {
            queryParameters.optimizer.searchWord(
                word, effectiveSearchScope, searchContext, false, queryParameters.elementToSearch
            )
        }
    }

    private fun getKeyName(elementToSearch: PsiElement): SearchTextHolder? {
        val resolvedText = when (elementToSearch) {
            is IProperty -> elementToSearch.key
            is YAMLKeyValue -> YAMLUtil.getConfigFullName(elementToSearch)
            else -> null
        } ?: return null

        val originalText = elementToSearch.getUserData(PROPERTY_REFERENCE_ORIGINAL_TEXT) ?: resolvedText
        return SearchTextHolder(resolvedText, originalText)
    }

    private fun getAdditionalWordsToSearch(searchTextHolder: SearchTextHolder): List<String> {
        return setOf(
            searchTextHolder.originalText.lowercase(),
            PropertyUtil.toCommonPropertyForm(searchTextHolder.originalText).lowercase(),
            tryAddDashToWord(searchTextHolder.resolvedText).lowercase(),
            tryAddDashToWord(searchTextHolder.originalText).lowercase()
        )
            .filter { !it.equals(searchTextHolder.resolvedText, ignoreCase = true) }
    }

    @VisibleForTesting
    fun getAdditionalWordsToSearch(resolvedText: String, originalText: String) =
        getAdditionalWordsToSearch(SearchTextHolder(resolvedText, originalText))

    private fun tryAddDashToWord(word: String): String {
        val toCharArray = word.toCharArray()
        val result = mutableListOf<Char>()
        for ((index, c) in toCharArray.withIndex()) {
            if (c.isUpperCase() && index > 0
                && toCharArray[index - 1] != '-' && toCharArray[index - 1] != '.' && toCharArray[index - 1] != '_'
            ) {
                result.add('-')
            }
            result.add(c)
        }
        return result.joinToString(separator = "")
    }
}

private data class SearchTextHolder(val resolvedText: String, val originalText: String)