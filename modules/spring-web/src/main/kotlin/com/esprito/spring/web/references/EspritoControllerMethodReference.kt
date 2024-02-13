package com.esprito.spring.web.references

import com.esprito.spring.web.service.beans.discoverer.SpringWebSearchService
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

class EspritoControllerMethodReference(
    element: PsiElement,
    private val urlPath: String,
    private val requestMethod: String,
    rangeInElement: TextRange
) : PsiReferenceBase<PsiElement>(element, rangeInElement), PsiPolyVariantReference, HighlightedReference {
    private val webSearchService = SpringWebSearchService.getInstance(element.project)
    private val module = lazy { ModuleUtilCore.findModuleForPsiElement(element) }

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val currentModule = module.value ?: return emptyArray()

        return webSearchService.getEndpointElements(urlPath, currentModule).asSequence()
            .filter { it.requestMethods.contains(requestMethod) }
            .mapTo(mutableListOf()) { PsiElementResolveResult(it.psiElement) }
            .toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        val currentModule = module.value ?: return emptyArray()

        return webSearchService.searchEndpoints(currentModule).asSequence()
            .filter { it.path.isNotEmpty() }
            .filter { it.requestMethods.contains(requestMethod) }
            .mapTo(mutableListOf()) {
                LookupElementBuilder.create(it, it.path)
                    .withRenderer(EndpointRenderer())
            }
            .toTypedArray()
    }

}