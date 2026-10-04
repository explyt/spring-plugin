/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.MappingPathPlaceholders
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.WebApplicationStack
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

class SpringWebControllerLoader(private val project: Project) : SpringWebEndpointsLoader {

    private val cachedValuesManager = CachedValuesManager.getManager(project)

    override fun isApplicable(module: Module) = SpringWebUtil.isWebModule(module)

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                doSearchEndpoints(module),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    /**
     * The type this loader registers under. The annotations of a controller are the same on both stacks, so each of
     * its endpoints carries the type of the stack its application runs on: see [endpointTypeOf].
     */
    override fun getType(): EndpointType {
        return EndpointType.SPRING_MVC
    }

    /**
     * `SPRING_WEBFLUX` for an application that runs reactive, and `SPRING_MVC` otherwise: a classpath with both stacks
     * runs servlet MVC, the way Spring Boot decides it.
     */
    private fun endpointTypeOf(module: Module): EndpointType =
        if (WebApplicationStack.of(module) == WebApplicationStack.REACTIVE) EndpointType.SPRING_WEBFLUX
        else EndpointType.SPRING_MVC

    private fun doSearchEndpoints(module: Module): List<EndpointElement> {
        val controllerAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringWebClasses.CONTROLLER, false
        ).takeIf { it.isNotEmpty() } ?: return emptyList()

        val allAnnotations = controllerAnnotations + MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringWebClasses.SWAGGER_API, false
        )
        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        val endpointType = endpointTypeOf(module)

        return allAnnotations.asSequence().flatMap { searchAnnotatedClasses(it, module) }
            .flatMap { getEndpoints(it, requestMappingMah, module, endpointType) }
            .toList()
    }

    private fun getEndpoints(
        controllerPsiClass: PsiClass,
        requestMappingMah: MetaAnnotationsHolder,
        module: Module,
        endpointType: EndpointType,
    ): List<EndpointElement> {
        val prefixes = requestMappingMah.getAnnotationMemberValues(controllerPsiClass, TARGET_VALUE)
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .ifEmpty { listOf("") }

        val result = mutableListOf<EndpointElement>()

        for (method in controllerPsiClass.allMethods) {
            if (!method.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) continue

            val mapping = SpringWebUtil.requestMappingOf(method, requestMappingMah)

            for (value in mapping.paths) {
                for (prefix in prefixes) {
                    val declared = "$prefix/$value"
                    result += EndpointElement(
                        SpringWebUtil.simplifyUrl(MappingPathPlaceholders.resolve(module, declared)),
                        mapping.methods,
                        method,
                        controllerPsiClass,
                        null,
                        endpointType,
                        pathTemplate = SpringWebUtil.simplifyUrl(declared),
                    )
                }
            }
        }
        return result
    }

    companion object {
        private val TARGET_VALUE = setOf("value")
    }
}