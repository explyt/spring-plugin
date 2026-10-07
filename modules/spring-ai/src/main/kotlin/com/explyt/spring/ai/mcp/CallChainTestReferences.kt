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
import com.intellij.psi.util.PsiTreeUtil

/**
 * The test code that depends on the methods of a call chain, and so breaks when one of them changes.
 *
 * A test rarely names the implementation a trace reaches: it mocks or calls the interface the bean is injected by,
 * and a web test sends a request to the handler's URL instead of calling the handler. Both are searched - the
 * interface declarations the call chain reached a method through, and the test requests whose URL matches the
 * endpoint the chain started from: MockMvc to any host, `WebTestClient`, and `java.net.http`, `RestTemplate`,
 * `TestRestTemplate` and `RestClient` to this machine. A URL match is weaker evidence than a resolved reference and is
 * kept apart from it.
 */
internal class CallChainTestReferences(private val module: Module, private val project: Project) {

    private val testScope = module.moduleTestsWithDependentsScope
    private val fileIndex = ProjectFileIndex.getInstance(project)

    /** The tests of one traced method: the references to it, and - on a handler - the requests to its URL. */
    data class NodeTests(val references: List<NodeTestReferenceJson>, val urlReferences: List<UrlTestReferenceJson>?)

    /**
     * The tests of [method], with the requests to its URL when [withUrlReferences] is set.
     *
     * A reference found both ways is a direct one: a search for an interface method also returns the calls written
     * against its implementations, and listing such a call twice would count one test as two.
     *
     * A request found by URL is not a reference to the handler, even when a URL reference resolves to it - the
     * bundled JetBrains Spring MVC plugin puts one on the MockMvc URL string, so the same request line came back
     * from the reference search too and one test was counted twice.
     */
    fun of(
        method: PsiMethod,
        viaDeclarations: List<PsiMethod>,
        withUrlReferences: Boolean,
        route: FunctionalRouteTarget? = null,
    ): NodeTests {
        val requests = when {
            !withUrlReferences -> null
            route != null -> requestsTo(route)
            else -> requestsTo(method)
        }
        val requestElements = requests.orEmpty().flatMap { it.second }
        val notARequest = { reference: PsiElement -> requestElements.none { isWithin(reference, it) } }

        val direct = linesByFile(referencesOf(method).filter(notARequest))
        val directLines = direct.flatMap { (location, lines) -> lines.map { location to it } }.toSet()
        val throughInterfaces = viaDeclarations.flatMap { declaration ->
            val via = CallChainTracer.nameOf(declaration)
            linesByFile(referencesOf(declaration).filter(notARequest)).mapNotNull { (location, lines) ->
                lines.filter { (location to it) !in directLines }.takeIf { it.isNotEmpty() }
                    ?.let { nodeReference(location, it, via) }
            }
        }
        val references = (direct.map { (location, lines) -> nodeReference(location, lines, via = null) } + throughInterfaces)
            .sortedWith(compareBy({ it.filePath }, { it.via }))

        val urlReferences = requests
            ?.flatMap { (path, usages) ->
                linesByFile(usages).map { (location, lines) ->
                    UrlTestReferenceJson(location.filePath, location.library, lines, path)
                }
            }
            ?.distinct()
            ?.sortedBy { it.filePath }
        return NodeTests(references, urlReferences)
    }

    private fun nodeReference(location: McpSourceLocation, lines: List<Int>, via: String?) =
        NodeTestReferenceJson(location.filePath, location.library, lines, via)

    /** The requests tests send to the endpoints [handler] serves, by the endpoint's mapping path. */
    private fun requestsTo(handler: PsiMethod): List<Pair<String, List<PsiElement>>> {
        val psiManager = PsiManager.getInstance(project)
        val handlerDeclaration = handler.navigationElement
        return SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { endpoint -> psiManager.areElementsEquivalent(endpoint.psiElement.navigationElement, handlerDeclaration) }
            .map { endpoint ->
                endpoint.path to EndpointUsageSearcher.findTestRequestUsage(endpoint.path, endpoint.requestMethods, module)
            }
    }

    private fun requestsTo(route: FunctionalRouteTarget): List<Pair<String, List<PsiElement>>>? {
        val verb = route.verb ?: return null
        val paths = route.paths.takeIf { it.isNotEmpty() } ?: return null
        return paths.map { path -> path to EndpointUsageSearcher.findTestRequestUsage(path, listOf(verb), module) }
    }

    /** Whether [reference] is part of the request [request] - its URL argument, or anything else inside the call. */
    private fun isWithin(reference: PsiElement, request: PsiElement): Boolean =
        request.containingFile == reference.containingFile && PsiTreeUtil.isAncestor(request, reference, false)

    private fun referencesOf(method: PsiMethod): List<PsiElement> =
        MethodReferencesSearch.search(method, testScope, true).findAll().map { it.element }

    private fun linesByFile(elements: List<PsiElement>): List<Pair<McpSourceLocation, List<Int>>> =
        elements.mapNotNull { element ->
            ProgressManager.checkCanceled()
            val file = element.containingFile?.virtualFile?.takeIf(fileIndex::isInTestSourceContent) ?: return@mapNotNull null
            val line = McpSourcePositions.sourceAnchorOf(element)?.let(McpSourcePositions::lineOfAnchor) ?: return@mapNotNull null
            McpSourceLocation.of(file, project) to line
        }
            .groupBy({ it.first }, { it.second })
            .map { (location, lines) -> location to lines.distinct().sorted() }
            .sortedBy { (location, _) -> location.filePath ?: location.library }
}
