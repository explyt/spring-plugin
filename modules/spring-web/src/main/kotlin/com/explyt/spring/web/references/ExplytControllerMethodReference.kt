/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.references

import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.ApplicationModules
import com.explyt.spring.web.util.EndpointPathPatterns
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.Module
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

        return webSearchService.getAllEndpointElements(urlPath, currentModule, URL_REFERENCE_TYPES)
            .filter { isRequestedMethod(it) }
            .groupBy { it.application }.values
            .flatMap { it.dispatchedFirst() }
            .distinctBy { it.psiElement }
            .mapTo(mutableListOf()) { PsiElementResolveResult(it.psiElement) }
            .toTypedArray()
    }

    private fun List<EndpointElement>.dispatchedFirst(): List<EndpointElement> {
        val (catchAll, specific) = partition { EndpointPathPatterns.isCatchAll(it.path) }
        return specific.ifEmpty { catchAll }
    }

    override fun getVariants(): Array<Any> {
        val currentModule = module.value ?: return emptyArray()
        val web = webSearchService.getAllEndpoints(currentModule, WEB_TYPES).asSequence()
            .filter { it.path.isNotEmpty() }
            .filter { isRequestedMethod(it) }
        return (web + actuatorVariants(currentModule))
            .mapTo(mutableListOf()) {
                LookupElementBuilder.create(it, it.path)
                    .withRenderer(EndpointRenderer())
            }
            .toTypedArray()
    }

    private fun actuatorVariants(currentModule: Module): Sequence<EndpointElement> =
        ApplicationModules.servingModulesOf(currentModule).ifEmpty { listOf(currentModule) }.asSequence()
            .flatMap { webSearchService.getAllEndpoints(it, ACTUATOR_TYPES) }
            .filter { it.path.isNotEmpty() }
            .filter { isRequestedMethod(it) }
            .distinctBy { it.path }

    private fun isRequestedMethod(it: EndpointElement) =
        requestMethod == null || it.requestMethods.contains(requestMethod)

}

private val WEB_TYPES = listOf(EndpointType.SPRING_MVC, EndpointType.SPRING_WEBFLUX)
private val ACTUATOR_TYPES = listOf(EndpointType.ACTUATOR)
private val URL_REFERENCE_TYPES = WEB_TYPES + ACTUATOR_TYPES