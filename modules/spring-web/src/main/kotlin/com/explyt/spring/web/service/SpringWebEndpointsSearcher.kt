/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.service

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.web.WebEeClasses
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.explyt.util.CacheUtils
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

@Service(Service.Level.PROJECT)
class SpringWebEndpointsSearcher(private val project: Project) {
    companion object {
        fun getInstance(project: Project): SpringWebEndpointsSearcher = project.service()
    }

    fun getLoadersTypes(): Collection<EndpointType> {
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(project)
            .map { it.getType() }
            .distinct()
    }

    fun getAllEndpoints(module: Module, types: List<EndpointType> = emptyList()): List<EndpointElement> {
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project).asSequence()
            .filter { types.isEmpty() || types.contains(it.getType()) }
            .filter { it.isApplicable(module) }
            .flatMapTo(mutableListOf()) { it.searchEndpoints(module) }
    }

    /**
     * Every endpoint of the project, each once. A controller found again from a dependent module is the same endpoint;
     * a mapping two controllers inherit from one base class is two, because Spring registers the handler methods of
     * each controller bean; an Actuator endpoint listed by two applications is two, because each application decides
     * its own exposure and access, whether the endpoint is a built-in one or declared in a library both reach. So
     * identity is the route, the declaration, the class serving it and the module owning it - never a verdict.
     */
    fun getAllEndpoints(): List<EndpointElement> {
        val seen = mutableSetOf<EndpointIdentity>()
        return project.modules.flatMapTo(mutableListOf()) { module ->
            getAllEndpoints(module).filter { seen.add(EndpointIdentity.of(it, module)) }
        }
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

    fun getJaxRsApplicationPath(module: Module): String {
        return CacheUtils.getCachedValue(
            module,
            ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
        ) {
            val applicationPathTargetClass = WebEeClasses.JAX_RS_APPLICATION_PATH.getTargetClass(module)
            val applicationPathMah = MetaAnnotationsHolder.of(module, applicationPathTargetClass)

            val httpAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
                module, applicationPathTargetClass, false
            ).takeIf { it.isNotEmpty() } ?: emptyList()

            httpAnnotations.asSequence()
                .flatMap {
                    SpringSearchService.getInstance(module.project)
                        .searchAnnotatedClasses(it, module)
                }
                .flatMap { applicationPathMah.getAnnotationMemberValues(it, setOf("value")) }
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
                .filter { it.isNotBlank() }
                .firstOrNull() ?: ""
        }
    }

}

private data class EndpointIdentity(
    val type: EndpointType,
    val path: String,
    val requestMethods: List<String>,
    val psiElement: PsiElement,
    val containingClass: PsiClass?,
    val owner: Module,
) {
    companion object {
        /**
         * An Actuator endpoint belongs to the application listing it, the one deciding its verdict. Any other endpoint
         * belongs to the module declaring it, so a dependent module listing it again lists the same endpoint; a library
         * declaration has no module of its own and belongs to the module listing it.
         */
        fun of(endpoint: EndpointElement, listedBy: Module): EndpointIdentity {
            val owner = if (endpoint.type == EndpointType.ACTUATOR) {
                listedBy
            } else {
                ModuleUtilCore.findModuleForPsiElement(endpoint.psiElement) ?: listedBy
            }
            return EndpointIdentity(
                endpoint.type, endpoint.path, endpoint.requestMethods, endpoint.psiElement, endpoint.containingClass, owner
            )
        }
    }
}
