/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.references

import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

class ExplytControllerMethodReference(
    element: PsiElement,
    private val urlPath: String,
    private val requestMethod: String?,
    rangeInElement: TextRange,
    isSoft: Boolean = false
) : PsiReferenceBase<PsiElement>(element, rangeInElement, isSoft), PsiPolyVariantReference, HighlightedReference {
    private val webSearchService = SpringWebEndpointsSearcher.getInstance(element.project)
    private val module = lazy { ModuleUtilCore.findModuleForPsiElement(element) }

    override fun isReferenceTo(element: PsiElement): Boolean {
        return false // it won't let you rename
    }

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val currentModule = module.value ?: return emptyArray()

        return webSearchService.getAllEndpointElements(
            urlPath,
            currentModule,
            listOf(EndpointType.SPRING_MVC, EndpointType.SPRING_WEBFLUX)
        ).asSequence()
            .filter { isRequestedMethod(it) }
            .mapTo(mutableListOf()) { PsiElementResolveResult(it.psiElement) }
            .toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        val currentModule = module.value ?: return emptyArray()
        return SpringWebEndpointsSearcher.getInstance(currentModule.project)
            .getAllEndpoints(currentModule, listOf(EndpointType.SPRING_MVC, EndpointType.SPRING_WEBFLUX)).asSequence()
            .filter { it.path.isNotEmpty() }
            .filter { isRequestedMethod(it) }
            .mapTo(mutableListOf()) {
                LookupElementBuilder.create(it, it.path)
                    .withRenderer(EndpointRenderer())
            }
            .toTypedArray()
    }

    private fun isRequestedMethod(it: EndpointElement) =
        requestMethod == null || it.requestMethods.contains(requestMethod)

}