/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

class SpringHttpExchangeLoader(private val project: Project) : SpringWebEndpointsLoader {

    private val cachedValuesManager = CachedValuesManager.getManager(project)

    override fun isApplicable(module: Module) = SpringWebUtil.isExchangeWebModule(module)

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                doSearchEndpoints(module),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    override fun getType(): EndpointType {
        return EndpointType.SPRING_HTTP_EXCHANGE
    }

    private fun doSearchEndpoints(module: Module): List<EndpointElement> {
        val httpAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringWebClasses.HTTP_EXCHANGE, false
        ).takeIf { it.isNotEmpty() } ?: return emptyList()

        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.HTTP_EXCHANGE)

        val processedClasses = mutableSetOf<String>()
        val endpoints = mutableListOf<EndpointElement>()

        for (annotation in httpAnnotations) {
            val classes = searchAnnotatedClasses(annotation, module) +
                    //HTTP exchange methods could be found in classes that are not annotated
                    searchAnnotatedMethods(annotation, module).mapNotNull { it.containingClass }

            for (psiClass in classes) {
                val classFqn = psiClass.qualifiedName ?: continue
                if (processedClasses.contains(classFqn)) continue
                processedClasses.add(classFqn)

                endpoints.addAll(getEndpoints(psiClass, requestMappingMah))
            }
        }

        return endpoints
    }

    private fun getEndpoints(
        annotatedClass: PsiClass, requestMappingMah: MetaAnnotationsHolder
    ): List<EndpointElement> {
        val prefixes = requestMappingMah.getAnnotationMemberValues(annotatedClass, TARGET_VALUE)
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .ifEmpty { listOf("") }

        val result = mutableListOf<EndpointElement>()

        for (method in annotatedClass.allMethods) {
            if (!method.isMetaAnnotatedBy(SpringWebClasses.HTTP_EXCHANGE)) continue

            val annotationMemberValues = requestMappingMah.getAnnotationMemberValues(method, TARGET_VALUE)
            val values = if (annotationMemberValues.isEmpty()) {
                listOf("")
            } else {
                annotationMemberValues.mapNotNull {
                    AnnotationUtil.getStringAttributeValue(it)
                }
            }

            val requestMethods = requestMappingMah.getAnnotationMemberValues(method, TARGET_METHOD)
                .map { ExplytPsiUtil.getUnquotedText(it) }

            for (value in values) {
                for (prefix in prefixes) {
                    result += EndpointElement(
                        SpringWebUtil.simplifyUrl("$prefix/$value"),
                        requestMethods,
                        method,
                        annotatedClass,
                        null,
                        getType()
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