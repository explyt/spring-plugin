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
import com.explyt.util.MultiVendorClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

class JaxRsExchangeLoader(private val project: Project) : SpringWebEndpointsLoader {

    private val cachedValuesManager = CachedValuesManager.getManager(project)

    override fun isApplicable(module: Module) = SpringWebUtil.isRsWebModule(module) ||
            WebEeClasses.JAX_RS_PATH.allFqns.any {
                JavaPsiFacade.getInstance(project).findClass(it, module.getModuleWithDependenciesAndLibrariesScope(false)) != null
            }

    private fun targetClassOf(vendorClass: MultiVendorClass, module: Module?): String {
        module ?: return vendorClass.jakarta
        val scope = module.getModuleWithDependenciesAndLibrariesScope(false)
        return if (JavaPsiFacade.getInstance(project).findClass(vendorClass.jakarta, scope) != null) vendorClass.jakarta
        else vendorClass.javax
    }

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
        val httpMethodTargetClass = targetClassOf(WebEeClasses.JAX_RS_HTTP_METHOD, module)
        val httpMethodAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, httpMethodTargetClass, false
        ).takeIf { it.isNotEmpty() } ?: return emptyList()

        val pathTargetClass = targetClassOf(WebEeClasses.JAX_RS_PATH, module)
        val pathMah = MetaAnnotationsHolder.of(module, pathTargetClass)
        val httpMethodMah = MetaAnnotationsHolder.of(module, httpMethodTargetClass)

        val processedClasses = mutableSetOf<String>()
        val endpoints = mutableListOf<EndpointElement>()

        val annotatedClasses = httpMethodAnnotations.asSequence()
            .flatMap { searchAnnotatedMethods(it, module) }
            .mapNotNull { it.containingClass }
            .distinct()
            .toList()

        for (annotatedClass in annotatedClasses) {
            for (psiClass in resourceClassesOf(annotatedClass, module)) {
                val classFqn = psiClass.qualifiedName ?: continue
                if (!processedClasses.add(classFqn)) continue

                endpoints.addAll(getEndpoints(psiClass, pathMah, httpMethodMah, applicationPath))
            }
        }

        return endpoints
    }

    private fun resourceClassesOf(annotatedClass: PsiClass, module: Module): List<PsiClass> {
        if (!annotatedClass.isAbstractType()) return listOf(annotatedClass)
        val pathTargetClass = targetClassOf(WebEeClasses.JAX_RS_PATH, module)
        val moduleScope = GlobalSearchScope.moduleScope(module)
        val implementations = ClassInheritorsSearch.search(annotatedClass, moduleScope, true)
            .filter { !it.isAbstractType() }
            .filter { implementation ->
                HandlerMethods.mappedType(implementation, HandlerMethods.HierarchyOrder.SUPERCLASS_FIRST) {
                    it.isMetaAnnotatedBy(pathTargetClass)
                } != null
            }
        if (implementations.isNotEmpty()) return implementations

        val declaredHere = annotatedClass.containingFile?.virtualFile?.let { moduleScope.contains(it) } ?: false
        val implementedInProject = ClassInheritorsSearch.search(annotatedClass, GlobalSearchScope.projectScope(project), true)
            .any { !it.isAbstractType() }
        return if (declaredHere && !implementedInProject) listOf(annotatedClass) else emptyList()
    }

    private fun PsiClass.isAbstractType() = isInterface || hasModifierProperty(PsiModifier.ABSTRACT)

    private fun getEndpoints(
        resourceClass: PsiClass,
        pathMah: MetaAnnotationsHolder,
        httpMethodMah: MetaAnnotationsHolder,
        applicationPath: String
    ): List<EndpointElement> {
        val module = ModuleUtil.findModuleForPsiElement(resourceClass)
        val pathTargetClass = targetClassOf(WebEeClasses.JAX_RS_PATH, module)
        val httpMethodTargetClass = targetClassOf(WebEeClasses.JAX_RS_HTTP_METHOD, module)

        val prefixes = HandlerMethods.mappedType(resourceClass, HandlerMethods.HierarchyOrder.SUPERCLASS_FIRST) {
            it.isMetaAnnotatedBy(pathTargetClass)
        }
            ?.let { pathMah.getAnnotationMemberValues(it, TARGET_VALUE) }.orEmpty()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .ifEmpty { listOf("") }

        val result = mutableListOf<EndpointElement>()
        val mappedMethods = HandlerMethods.mappedMethods(resourceClass, HandlerMethods.HierarchyOrder.SUPERCLASS_FIRST) {
            it.isMetaAnnotatedBy(httpMethodTargetClass)
        }
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