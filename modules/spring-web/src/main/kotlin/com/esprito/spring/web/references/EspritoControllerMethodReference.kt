package com.esprito.spring.web.references

import com.esprito.spring.web.service.beans.discoverer.EndpointElement
import com.esprito.spring.web.service.beans.discoverer.SpringWebEndpointsSearcher
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

class EspritoControllerMethodReference(
    element: PsiElement,
    private val urlPath: String,
    private val requestMethod: String?,
    rangeInElement: TextRange
) : PsiReferenceBase<PsiElement>(element, rangeInElement), PsiPolyVariantReference, HighlightedReference {
    private val webSearchService = SpringWebEndpointsSearcher.getInstance(element.project)
    private val module = lazy { ModuleUtilCore.findModuleForPsiElement(element) }

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val currentModule = module.value ?: return emptyArray()

        return webSearchService.getAllEndpointElements(urlPath, currentModule).asSequence()
            .filter { isRequestedMethod(it) }
            .mapTo(mutableListOf()) { PsiElementResolveResult(it.psiElement) }
            .toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        val currentModule = module.value ?: return emptyArray()
        val endpoints = SpringWebEndpointsSearcher.getInstance(currentModule.project).getAllEndpoints(currentModule)
        return endpoints.asSequence()
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