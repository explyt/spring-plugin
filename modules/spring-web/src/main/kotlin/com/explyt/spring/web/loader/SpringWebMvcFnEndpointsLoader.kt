/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

/**
 * Functional routes of the servlet stack (WebMvc.fn). They are declared with the same DSL and builder as the reactive
 * ones, but a Spring MVC project carries no Reactor, so they cannot be discovered by the WebFlux loader — its
 * applicability check looks for `reactor.core.publisher.Flux`.
 */
class SpringWebMvcFnEndpointsLoader(private val project: Project) : SpringWebEndpointsLoader {
    private val cachedValuesManager = CachedValuesManager.getManager(project)
    private val handler = EndpointHandlerChain(
        listOf(
            SpringWebRouterFunctionLoader(),
            SpringWebCoRouterLoader()
        )
    )

    override fun isApplicable(module: Module) = SpringWebUtil.isWebMvcFnModule(module)

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                doSearchEndpoints(module),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    override fun getType(): EndpointType {
        return EndpointType.SPRING_MVC
    }

    private fun doSearchEndpoints(module: Module): List<EndpointElement> {
        val componentAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringCoreClasses.COMPONENT, false
        )

        return componentAnnotations.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, module.moduleWithDependenciesScope) }
            .flatMap { handler.handleEndpoints(it) }
            .filter { it.type == EndpointType.SPRING_MVC }
            .toList()
    }
}
