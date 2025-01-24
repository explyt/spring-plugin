/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.web.service

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.explyt.util.CacheUtils
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules

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
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project)
            .filter { types.isEmpty() || types.contains(it.getType()) }
            .flatMapTo(mutableListOf()) { it.searchEndpoints(module) }
    }

    fun getAllEndpoints(): List<EndpointElement> {
        val distinctElements = mutableSetOf<EndpointElement>()

        return project.modules.flatMapTo(mutableListOf()) { module ->
            getAllEndpoints(module)
                .filter { endpoint: EndpointElement ->
                    val isUnique = !distinctElements.contains(endpoint)
                    distinctElements.add(endpoint)
                    isUnique
                }
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
            val applicationPathMah = MetaAnnotationsHolder.of(module, SpringWebClasses.JAX_RS_APPLICATION_PATH)

            val httpAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
                module, SpringWebClasses.JAX_RS_APPLICATION_PATH, false
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