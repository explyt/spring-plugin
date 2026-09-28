/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.util.CacheKeyStore
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

/**
 * Functional routes (`coRouter`/`router` DSL and the `RouterFunctions` builder) of one web stack.
 *
 * The reactive and servlet stacks declare routes the same way and share one handler chain, which returns the routes
 * of **both**. Each loader therefore keeps only the routes of the [type] it reports: without that filter a project
 * carrying both stacks would list every route twice.
 */
abstract class FunctionalRouteEndpointsLoader(
    private val project: Project,
    private val type: EndpointType
) : SpringWebEndpointsLoader {

    private val handler = EndpointHandlerChain(
        listOf(
            SpringWebRouterFunctionLoader(),
            SpringWebCoRouterLoader()
        )
    )

    final override fun getType(): EndpointType = type

    /**
     * Cached under a key of its own [type]: both loaders share this provider lambda, and the platform keys a cache by
     * the lambda class, so a shared key would answer one stack's routes from the other's cache.
     */
    final override fun searchEndpoints(module: Module): List<EndpointElement> {
        val key = CacheKeyStore.getInstance(project).getKey<List<EndpointElement>>("FunctionalRouteEndpoints($type)")
        return CachedValuesManager.getManager(project).getCachedValue(module, key, {
            CachedValueProvider.Result(
                doSearchEndpoints(module),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }, false)
    }

    private fun doSearchEndpoints(module: Module): List<EndpointElement> {
        val componentAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringCoreClasses.COMPONENT, false
        )

        return componentAnnotations.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, module.moduleWithDependenciesScope) }
            .flatMap { handler.handleEndpoints(it) }
            .filter { it.type == type }
            .toList()
    }
}
