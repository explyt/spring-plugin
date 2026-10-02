/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.web.util.ApplicationBasePath
import com.explyt.spring.web.util.EndpointUrlMatcher
import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.AnnotatedElementsSearch

interface SpringWebEndpointsLoader {

    fun searchEndpoints(module: Module): List<EndpointElement>

    fun getType(): EndpointType

    /**
     * The endpoints a URL written in code addresses: relative, absolute against this machine
     * (`http://localhost:8080/api/items`), or under the base path the endpoint's application declares. A URL naming
     * another host addresses another service, and nothing here.
     */
    fun getEndpointElements(urlPath: String, module: Module): List<EndpointElement> {
        if (!getType().isWeb) return emptyList()
        val basePaths = HashMap<Module, String?>()

        return EndpointUrlMatcher.match(
            searchEndpoints(module), urlPath, EndpointUrlMatcher.Policy.REFERENCE,
            routeOf = { it.path },
            basePathOf = { endpoint ->
                val owner = ModuleUtilCore.findModuleForPsiElement(endpoint.psiElement) ?: module
                basePaths.getOrPut(owner) { ApplicationBasePath.cachedOf(owner) }
            },
        ).endpoints
    }

    fun searchAnnotatedClasses(annotation: PsiClass, module: Module): List<PsiClass> =
        SpringSearchService.getInstance(module.project).searchAnnotatedClasses(annotation, module)

    fun searchAnnotatedMethods(annotation: PsiClass, module: Module): List<PsiMethod> {
        return AnnotatedElementsSearch.searchPsiMethods(annotation, module.moduleWithDependenciesScope).toList()
    }

    fun isApplicable(module: Module): Boolean

    companion object {
        val EP_NAME = ProjectExtensionPointName<SpringWebEndpointsLoader>(
            "com.explyt.spring.web.springWebEndpointsLoader"
        )

    }
}

data class Referrer(
    val path: String,
    val method: String?,
    val psiElement: PsiElement
)

/**
 * @property path the path the application serves, with configuration placeholders resolved.
 * @property pathTemplate the path as declared, placeholders included; equal to [path] when nothing was resolved.
 * @property exposure whether the configuration publishes the endpoint over HTTP; `null` for an endpoint that is
 * served whenever it is declared, which is every kind but an Actuator endpoint.
 */
data class EndpointElement(
    val path: String,
    val requestMethods: List<String>,
    val psiElement: PsiElement,
    val containingClass: PsiClass?,
    val containingFile: PsiFile?,
    val type: EndpointType,
    val pathTemplate: String = path,
    val exposure: EndpointExposure? = null,
)

/** Whether an Actuator endpoint answers over HTTP, as `management.endpoints.web.exposure` decides it. */
enum class EndpointExposure {
    EXPOSED,
    NOT_EXPOSED,

    /** The configuration holds a value the IDE cannot read, such as a placeholder no configuration file resolves. */
    UNKNOWN,
}

sealed class EndpointData {
    data class ReferrerData(val referrer: Referrer) : EndpointData()
    data class EndpointElementData(val endpointElement: EndpointElement) : EndpointData()
}

data class EndpointFileData(val psiFile: PsiFile, val endpoints: List<EndpointData>)

enum class EndpointType(val readable: String, val isWeb: Boolean) {
    SPRING_BOOT("Spring Boot", false),
    SPRING_MVC("Spring MVC", true),
    SPRING_HTTP_EXCHANGE("HttpExchange", true),
    SPRING_JAX_RS("JAX-RS", true),
    SPRING_WEBFLUX("WebFlux", true),
    OPENAPI("OpenAPI", false),
    SPRING_OPEN_FEIGN("OpenFeign", true),
    MESSAGE_BROKER("Message Broker", false),
    EVENT_LISTENERS("Event Listeners", false),
    ACTUATOR("Actuator", true),
}
