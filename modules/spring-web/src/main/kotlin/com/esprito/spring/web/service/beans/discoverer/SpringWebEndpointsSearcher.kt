package com.esprito.spring.web.service.beans.discoverer

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class SpringWebEndpointsSearcher {
    companion object {
        fun getInstance(project: Project): SpringWebEndpointsSearcher = project.service()
    }

    fun getAllEndpoints(module: Module): List<EndpointElement> {
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project)
            .flatMapTo(mutableListOf()) { it.searchEndpoints(module) }
    }

    fun getAllEndpointElements(urlPath: String, module: Module, type: EndpointType? = null): List<EndpointElement> {
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project)
            .filter { type == null || it.getType() == type }
            .flatMapTo(mutableListOf()) { it.getEndpointElements(urlPath, module) }
    }
}