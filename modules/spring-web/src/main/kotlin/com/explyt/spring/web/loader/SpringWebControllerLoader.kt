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

package com.explyt.spring.web.loader

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.SpringWebUtil
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

    override fun getType(): EndpointType {
        return EndpointType.SPRING_MVC
    }

    private fun doSearchEndpoints(module: Module): List<EndpointElement> {
        val controllerAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringWebClasses.CONTROLLER, false
        ).takeIf { it.isNotEmpty() } ?: return emptyList()

        val allAnnotations = controllerAnnotations + MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringWebClasses.SWAGGER_API, false
        )
        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)

        return allAnnotations.asSequence().flatMap { searchAnnotatedClasses(it, module) }
            .flatMap { getEndpoints(it, requestMappingMah) }
            .toList()
    }

    private fun getEndpoints(
        controllerPsiClass: PsiClass, requestMappingMah: MetaAnnotationsHolder
    ): List<EndpointElement> {
        val prefixes = requestMappingMah.getAnnotationMemberValues(controllerPsiClass, TARGET_VALUE)
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .ifEmpty { listOf("") }

        val result = mutableListOf<EndpointElement>()

        for (method in controllerPsiClass.allMethods) {
            if (!method.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) continue

            val annotationMemberValues = requestMappingMah.getAnnotationMemberValues(method, TARGET_VALUE)
            val values = if (annotationMemberValues.isEmpty()) {
                listOf("")
            } else {
                annotationMemberValues.mapNotNull {
                    AnnotationUtil.getStringAttributeValue(it)
                }
            }

            val requestMethods = requestMappingMah.getAnnotationMemberValues(method, TARGET_METHOD)
                .map { it.text.split('.').last() }

            for (value in values) {
                for (prefix in prefixes) {
                    result += EndpointElement(
                        SpringWebUtil.simplifyUrl("$prefix/$value"),
                        requestMethods,
                        method,
                        controllerPsiClass,
                        null,
                        EndpointType.SPRING_MVC
                    )
                }
            }
        }
        return result
    }

    companion object {
        private val TARGET_VALUE = setOf("value")
        private val TARGET_METHOD = setOf("method")
    }
}