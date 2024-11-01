package com.explyt.spring.web.loader

import com.explyt.spring.web.providers.EndpointUsageSearcher.getOpenApiJsonEndpoints
import com.explyt.spring.web.providers.EndpointUsageSearcher.getOpenApiYamlEndpoints
import com.explyt.spring.web.tracker.OpenApiLanguagesModificationTracker
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager


class SpringWebOpenApiEndpointsLoader(private val project: Project) : SpringWebEndpointsLoader {

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            val modificationTracker = project.getService(OpenApiLanguagesModificationTracker::class.java)
                ?: ModificationTracker.NEVER_CHANGED
            CachedValueProvider.Result(
                doSearchEndpoints(module),
                modificationTracker
            )
        }
    }

    override fun getType(): EndpointType {
        return EndpointType.OPENAPI
    }

    private fun doSearchEndpoints(module: Module): List<EndpointElement> {
        return listOf(
            getOpenApiJsonEndpoints(module),
            getOpenApiYamlEndpoints(module)
        )
            .flatMap { result ->
                result.filterIsInstance<EndpointData.EndpointElementData>()
                    .map { it.endpointElement }
            }
    }

}