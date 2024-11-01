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

package com.explyt.jpa.ql.reference.search

import com.explyt.jpa.ql.psi.JpqlFile
import com.explyt.jpa.ql.psi.JpqlIdentifier
import com.explyt.jpa.ql.psi.JpqlVisitor
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.cache.CacheManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor
import kotlin.experimental.or

abstract class JpqlQuerySearcherBase : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {
    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) = runReadAction {
        val cacheManager = CacheManager.getInstance(queryParameters.project)

        val elementToSearch = queryParameters.elementToSearch

        val (text, searchScope) = getSearchDetails(elementToSearch)
            ?: return@runReadAction

        cacheManager.processFilesWithWord(
            Processor {
                if (ApplicationManager.getApplication().isUnitTestMode
                    || ApplicationManager.getApplication().isInternal
                ) {
                    if (it is JpqlFile) {
                        processJpqlFile(it, elementToSearch, consumer, text)
                        return@Processor true
                    }
                }

                val uFile = it.toUElementOfType<UFile>() ?: return@Processor true
                processFile(uFile, consumer, elementToSearch, text)
                true
            },
            text,
            UsageSearchContext.IN_STRINGS.or(UsageSearchContext.IN_FOREIGN_LANGUAGES),
            searchScope,
            true
        )
    }

    protected data class SearchDetails(val text: String, val scope: GlobalSearchScope)

    protected abstract fun getSearchDetails(elementToSearch: PsiElement): SearchDetails?

    private fun processFile(
        uFile: UFile,
        consumer: Processor<in PsiReference>,
        elementToSearch: PsiElement,
        text: String
    ) {
        val injectedLanguageManager = InjectedLanguageManager.getInstance(elementToSearch.project)

        uFile.accept(object : AbstractUastVisitor() {
            override fun visitElement(node: UElement): Boolean {
                if (node is UInjectionHost) {
                    val jpqlFiles = injectedLanguageManager.getInjectedPsiFiles(node.psiLanguageInjectionHost)
                        ?.asSequence()
                        ?.mapNotNull { it.first as? JpqlFile }

                    jpqlFiles?.forEach { jpqlFile ->
                        processJpqlFile(jpqlFile, elementToSearch, consumer, text)
                    }
                }
                return false
            }
        })
    }

    private fun processJpqlFile(
        jpqlFile: JpqlFile,
        psiElement: PsiElement,
        consumer: Processor<in PsiReference>,
        text: String
    ) {
        jpqlFile.acceptChildren(object : JpqlVisitor() {
            override fun visitIdentifier(identifier: JpqlIdentifier) {
                if (identifier.name == text) {
                    val reference = identifier.reference
                    if (reference?.isReferenceTo(psiElement) == true) {
                        consumer.process(reference)
                    }
                }

                super.visitIdentifier(identifier)
            }

            override fun visitPsiElement(element: PsiElement) {
                element.acceptChildren(this)
                super.visitPsiElement(element)
            }
        })
    }
}