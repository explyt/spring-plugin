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