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
import com.explyt.spring.web.WebEeClasses
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

class JaxRsExchangeLoader(private val project: Project) : SpringWebEndpointsLoader {

    private val cachedValuesManager = CachedValuesManager.getManager(project)

    override fun isApplicable(module: Module) = SpringWebUtil.isRsWebModule(module)

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                doSearchEndpoints(module),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    override fun getType(): EndpointType {
        return EndpointType.SPRING_JAX_RS
    }

    private fun doSearchEndpoints(module: Module): List<EndpointElement> {
        val applicationPath = SpringWebEndpointsSearcher.getInstance(project).getJaxRsApplicationPath(module)
        val httpMethodTargetClass = WebEeClasses.JAX_RS_HTTP_METHOD.getTargetClass(module)
        val httpMethodAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, httpMethodTargetClass, false
        ).takeIf { it.isNotEmpty() } ?: return emptyList()

        val pathTargetClass = WebEeClasses.JAX_RS_PATH.getTargetClass(module)
        val pathMah = MetaAnnotationsHolder.of(module, pathTargetClass)
        val httpMethodMah = MetaAnnotationsHolder.of(module, httpMethodTargetClass)

        val processedClasses = mutableSetOf<String>()
        val endpoints = mutableListOf<EndpointElement>()

        for (annotation in httpMethodAnnotations) {
            val classes = searchAnnotatedMethods(annotation, module).mapNotNull { it.containingClass }

            for (psiClass in classes) {
                val classFqn = psiClass.qualifiedName ?: continue
                if (processedClasses.contains(classFqn)) continue
                processedClasses.add(classFqn)

                endpoints.addAll(getEndpoints(psiClass, pathMah, httpMethodMah, applicationPath))
            }
        }

        return endpoints
    }

    private fun getEndpoints(
        annotatedClass: PsiClass,
        pathMah: MetaAnnotationsHolder,
        httpMethodMah: MetaAnnotationsHolder,
        applicationPath: String
    ): List<EndpointElement> {
        val prefixes = pathMah.getAnnotationMemberValues(annotatedClass, TARGET_VALUE)
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .ifEmpty { listOf("") }

        val result = mutableListOf<EndpointElement>()
        val module = ModuleUtil.findModuleForPsiElement(annotatedClass)
        val httpMethodTargetClass = WebEeClasses.JAX_RS_HTTP_METHOD.getTargetClass(module)
        for (method in annotatedClass.allMethods) {
            if (!method.isMetaAnnotatedBy(httpMethodTargetClass)) continue

            val pathValues = pathMah.getAnnotationMemberValues(method, TARGET_VALUE)
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
                .ifEmpty { listOf("") }

            val requestMethods = httpMethodMah.getAnnotationMemberValues(method, TARGET_VALUE)
                .map { ExplytPsiUtil.getUnquotedText(it) }

            for (value in pathValues) {
                for (prefix in prefixes) {
                    result += EndpointElement(
                        SpringWebUtil.simplifyUrl("$applicationPath/$prefix/$value"),
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
    }
}