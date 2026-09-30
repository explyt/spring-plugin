/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.web.providers.EndpointUsageSearcher
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.MethodReferencesSearch

/**
 * The test code that depends on the methods of a call chain, and so breaks when one of them changes.
 *
 * A test rarely names the implementation a trace reaches: it mocks or calls the interface the bean is injected by,
 * and a web test sends a request to the handler's URL instead of calling the handler. Both are searched - the
 * interface declarations the call chain reached a method through, and MockMvc and WebTestClient requests whose URL
 * matches the endpoint the chain started from. A URL match is weaker evidence than a resolved reference and is kept
 * apart from it.
 */
internal class CallChainTestReferences(private val module: Module, private val project: Project) {

    private val testScope = module.moduleTestsWithDependentsScope
    private val fileIndex = ProjectFileIndex.getInstance(project)
    private val basePath = project.basePath

    /**
     * A reference found both ways is a direct one: a search for an interface method also returns the calls written
     * against its implementations, and listing such a call twice would count one test as two.
     */
    fun referencesTo(method: PsiMethod, viaDeclarations: List<PsiMethod>): List<NodeTestReferenceJson> {
        val direct = linesByFile(referencesOf(method))
        val directLines = direct.flatMap { (file, lines) -> lines.map { file to it } }.toSet()
        val throughInterfaces = viaDeclarations.flatMap { declaration ->
            val via = CallChainTracer.nameOf(declaration)
            linesByFile(referencesOf(declaration)).mapNotNull { (file, lines) ->
                lines.filter { (file to it) !in directLines }.takeIf { it.isNotEmpty() }
                    ?.let { NodeTestReferenceJson(file, it, via) }
            }
        }
        return (direct.map { (file, lines) -> NodeTestReferenceJson(file, lines, via = null) } + throughInterfaces)
            .sortedWith(compareBy({ it.filePath }, { it.via }))
    }

    fun urlReferencesTo(handler: PsiMethod): List<UrlTestReferenceJson> {
        val psiManager = PsiManager.getInstance(project)
        val handlerDeclaration = handler.navigationElement
        val endpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { endpoint -> psiManager.areElementsEquivalent(endpoint.psiElement.navigationElement, handlerDeclaration) }
        return endpoints.flatMap { endpoint ->
            val usages = EndpointUsageSearcher.findMockMvcEndpointUsage(endpoint.path, endpoint.requestMethods, module) +
                    EndpointUsageSearcher.findWebTestClientEndpointUsage(endpoint.path, endpoint.requestMethods, module)
            linesByFile(usages).map { (file, lines) -> UrlTestReferenceJson(file, lines, endpoint.path) }
        }.distinct().sortedBy { it.filePath }
    }

    private fun referencesOf(method: PsiMethod): List<PsiElement> =
        MethodReferencesSearch.search(method, testScope, true).findAll().map { it.element }

    private fun linesByFile(elements: List<PsiElement>): List<Pair<String, List<Int>>> =
        elements.mapNotNull { element ->
            ProgressManager.checkCanceled()
            val file = element.containingFile?.virtualFile?.takeIf(fileIndex::isInTestSourceContent) ?: return@mapNotNull null
            val line = McpSourcePositions.sourceAnchorOf(element)?.let(McpSourcePositions::lineOfAnchor) ?: return@mapNotNull null
            relativePathOf(file.path) to line
        }
            .groupBy({ it.first }, { it.second })
            .map { (file, lines) -> file to lines.distinct().sorted() }
            .sortedBy { it.first }

    private fun relativePathOf(path: String): String =
        basePath?.let { path.removePrefix("$it/") } ?: path
}
