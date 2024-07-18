package com.esprito.spring.web.service.beans.discoverer

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

class SpringWebBeanEndpointsLoader(private val project: Project) : SpringWebEndpointsLoader {
    private val cachedValuesManager = CachedValuesManager.getManager(project)
    private val handler = EndpointHandlerChain(
        listOf(
            SpringWebRouterFunctionLoader(),
            SpringWebCoRouterLoader()
        )
    )

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                doSearchEndpoints(module),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun doSearchEndpoints(module: Module): List<EndpointElement> {
        val componentAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringCoreClasses.COMPONENT, false
        )

        return componentAnnotations.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, module.moduleWithDependenciesScope) }
            .flatMap { handler.handleEndpoints(it) }
            .toList()
    }
}
