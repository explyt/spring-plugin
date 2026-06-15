/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.AnnotatedElementsSearch

interface SpringWebEndpointsLoader {

    fun searchEndpoints(module: Module): List<EndpointElement>

    fun getType(): EndpointType

    fun getEndpointElements(urlPath: String, module: Module): List<EndpointElement> {
        if (!getType().isWeb) return emptyList()
        val searchUrl = SpringWebUtil.simplifyUrl(urlPath)

        return searchEndpoints(module)
            .filter { SpringWebUtil.isEndpointMatches(it.path, searchUrl) }
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

data class EndpointElement(
    val path: String,
    val requestMethods: List<String>,
    val psiElement: PsiElement,
    val containingClass: PsiClass?,
    val containingFile: PsiFile?,
    val type: EndpointType
)

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
}
