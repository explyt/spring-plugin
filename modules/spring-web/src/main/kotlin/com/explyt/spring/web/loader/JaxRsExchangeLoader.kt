/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.web.WebEeClasses
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.HandlerMethods
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
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
                .flatMap { resourceClassesOf(it, module) }

            for (psiClass in classes) {
                val classFqn = psiClass.qualifiedName ?: continue
                if (processedClasses.contains(classFqn)) continue
                processedClasses.add(classFqn)

                endpoints.addAll(getEndpoints(psiClass, pathMah, httpMethodMah, applicationPath))
            }
        }

        return endpoints
    }

    private fun resourceClassesOf(annotatedClass: PsiClass, module: Module): List<PsiClass> {
        if (!annotatedClass.isInterface && !annotatedClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return listOf(annotatedClass)
        }
        val pathTargetClass = WebEeClasses.JAX_RS_PATH.getTargetClass(module)
        val implementations = ClassInheritorsSearch.search(annotatedClass, GlobalSearchScope.moduleScope(module), true)
            .filter { !it.isInterface && !it.hasModifierProperty(PsiModifier.ABSTRACT) }
            .filter { implementation ->
                HandlerMethods.mappedType(implementation) { it.isMetaAnnotatedBy(pathTargetClass) } != null
            }
        return implementations.ifEmpty { listOf(annotatedClass) }
    }

    private fun getEndpoints(
        resourceClass: PsiClass,
        pathMah: MetaAnnotationsHolder,
        httpMethodMah: MetaAnnotationsHolder,
        applicationPath: String
    ): List<EndpointElement> {
        val module = ModuleUtil.findModuleForPsiElement(resourceClass)
        val pathTargetClass = WebEeClasses.JAX_RS_PATH.getTargetClass(module)
        val httpMethodTargetClass = WebEeClasses.JAX_RS_HTTP_METHOD.getTargetClass(module)

        val prefixes = HandlerMethods.mappedType(resourceClass) { it.isMetaAnnotatedBy(pathTargetClass) }
            ?.let { pathMah.getAnnotationMemberValues(it, TARGET_VALUE) }.orEmpty()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .ifEmpty { listOf("") }

        val result = mutableListOf<EndpointElement>()
        val mappedMethods = HandlerMethods.mappedMethods(resourceClass) { it.isMetaAnnotatedBy(httpMethodTargetClass) }
        for ((method, mappingSource) in mappedMethods) {
            val pathValues = pathMah.getAnnotationMemberValues(mappingSource, TARGET_VALUE)
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
                .ifEmpty { listOf("") }

            val requestMethods = httpMethodMah.getAnnotationMemberValues(mappingSource, TARGET_VALUE)
                .map { ExplytPsiUtil.getUnquotedText(it) }

            for (value in pathValues) {
                for (prefix in prefixes) {
                    result += EndpointElement(
                        SpringWebUtil.simplifyUrl("$applicationPath/$prefix/$value"),
                        requestMethods,
                        method,
                        resourceClass,
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