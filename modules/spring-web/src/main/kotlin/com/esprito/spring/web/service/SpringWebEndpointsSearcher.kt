package com.esprito.spring.web.service

import com.esprito.spring.web.loader.EndpointElement
import com.esprito.spring.web.loader.EndpointType
import com.esprito.spring.web.loader.SpringWebEndpointsLoader
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class SpringWebEndpointsSearcher {
    companion object {
        fun getInstance(project: Project): SpringWebEndpointsSearcher = project.service()
    }

    fun getAllEndpoints(module: Module, types: List<EndpointType> = emptyList()): List<EndpointElement> {
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project)
            .filter { types.isEmpty() || types.contains(it.getType()) }
            .flatMapTo(mutableListOf()) { it.searchEndpoints(module) }
    }

    fun getAllEndpointElements(
        urlPath: String,
        module: Module,
        types: List<EndpointType> = emptyList()
    ): List<EndpointElement> {
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project)
            .filter { types.isEmpty() || types.contains(it.getType()) }
            .flatMapTo(mutableListOf()) { it.getEndpointElements(urlPath, module) }
    }
}