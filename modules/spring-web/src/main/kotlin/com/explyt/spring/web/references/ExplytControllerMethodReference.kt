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