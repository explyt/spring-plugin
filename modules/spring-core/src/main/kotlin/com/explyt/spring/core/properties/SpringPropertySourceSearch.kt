/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties

import com.explyt.module.ExternalSystemModule
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.util.ExplytAnnotationUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.AnnotatedElementsSearch

@Service(Service.Level.PROJECT)
class SpringPropertySourceSearch(private val project: Project) {

    companion object {
        fun getInstance(project: Project): SpringPropertySourceSearch = project.service()
    }

    fun findPropertySourceFilePaths(targetElement: PsiElement): Set<String> {
        val module = ModuleUtilCore.findModuleForPsiElement(targetElement) ?: return emptySet()
        val externalSystemModule = ExternalSystemModule.of(module)
        val moduleProductionSourceScope = externalSystemModule.mainModule?.moduleWithDependenciesScope ?: return emptySet()

        val librariesSearchScope = externalSystemModule.librariesSearchScope
        val javaPsiFacade = JavaPsiFacade.getInstance(project)

        val propertySourcesAnn =
            javaPsiFacade.findClass(SpringCoreClasses.PROPERTY_SOURCES, librariesSearchScope) ?: return emptySet()
        val propertySourceAnn =
            javaPsiFacade.findClass(SpringCoreClasses.PROPERTY_SOURCE, librariesSearchScope) ?: return emptySet()


        val allPropertyPaths = mutableSetOf<String>()

        AnnotatedElementsSearch.searchPsiClasses(propertySourceAnn, moduleProductionSourceScope)
            .flatMapTo(allPropertyPaths) { annotatedElement ->
                val propertySourceAnnotations = AnnotationUtil
                    .findAllAnnotations(annotatedElement, listOf(SpringCoreClasses.PROPERTY_SOURCE), false)
                propertySourceAnnotations.flatMap { propertySourceAnn ->
                    ExplytAnnotationUtil.getArrayAttributeValue(propertySourceAnn, "value")
                }
            }

        AnnotatedElementsSearch.searchPsiClasses(propertySourcesAnn, moduleProductionSourceScope)
            .flatMapTo(allPropertyPaths) { annotatedElement ->
                val propertySourcesAnnotation = AnnotationUtil
                    .findAnnotation(annotatedElement, listOf(SpringCoreClasses.PROPERTY_SOURCES), false)
                    ?: return emptySet()
                ExplytAnnotationUtil.getArrayValueAnnotations(propertySourcesAnnotation, "value").flatMap {
                    ExplytAnnotationUtil.getArrayAttributeValue(it, "value")
                }
            }

        return allPropertyPaths
    }
}