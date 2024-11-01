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

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.UastUtil.getPropertyValue
import com.explyt.spring.web.SpringWebClasses
import com.explyt.util.ExplytAnnotationUtil.getStringValue
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.toUElementOfType

class SpringWebFeignClientLoader(val project: Project) : SpringWebEndpointsLoader {
    private val cachedValuesManager = CachedValuesManager.getManager(project)

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                doSearchEndpoints(module),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    override fun getType(): EndpointType {
        return EndpointType.SPRING_OPEN_FEIGN
    }

    private fun doSearchEndpoints(module: Module): List<EndpointElement> {
        val feignAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringWebClasses.FEIGN_CLIENT, false
        )

        val scope = ProjectScope.getProjectScope(project)

        return feignAnnotations.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, scope) }
            .filterNotNull()
            .flatMap { getEndpoints(module, it) }
            .toList()
    }

    private fun getPrefix(module: Module, psiClass: PsiClass): String {
        val metaAnnotationsHolder = MetaAnnotationsHolder.of(module, SpringWebClasses.FEIGN_CLIENT)
        val annotation = psiClass.annotations.find { metaAnnotationsHolder.contains(it) } ?: return ""
        val urlAnnotation = metaAnnotationsHolder.getAnnotationMemberValues(annotation, setOf("url"))
            .firstOrNull()

        val urlExpression = urlAnnotation.toUElementOfType<UExpression>()
        var url = urlExpression?.getPropertyValue() ?: urlAnnotation?.getStringValue() ?: ""

        if (url.isEmpty()) {
            url = getUrlFromProperty(module, metaAnnotationsHolder, annotation)
        }

        val path = metaAnnotationsHolder.getAnnotationMemberValues(annotation, setOf("path"))
            .firstOrNull()?.getStringValue() ?: ""
        return url + path
    }

    private fun getUrlFromProperty(
        module: Module,
        metaAnnotationsHolder: MetaAnnotationsHolder,
        annotation: PsiAnnotation
    ): String {
        val nameAnnotation = metaAnnotationsHolder.getAnnotationMemberValues(annotation, setOf("name", "value"))
            .firstOrNull() ?: return ""
        val feignName = AnnotationUtil.getStringAttributeValue(nameAnnotation) ?: return ""
        val urlProperties =
            DefinedConfigurationPropertiesSearch.getInstance(project).getAllProperties(module).stream()
                .filter { it.key == SpringWebClasses.OPEN_FEIGN_CLIENT_CONFIG + ".$feignName.url" }
                .map { it.value }
                .toList()
        if (urlProperties.size != 1) return ""

        return urlProperties.firstOrNull() ?: ""
    }

    private fun getEndpoints(module: Module, psiClass: PsiClass): List<EndpointElement> {
        val prefix = getPrefix(module, psiClass)
        val uClass = psiClass.toUElementOfType<UClass>() ?: return emptyList()
        val requestMapping = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)

        val result = mutableListOf<EndpointElement>()

        for (method in uClass.allMethods) {
            if (!method.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) continue

            val annotationMemberValues = requestMapping.getAnnotationMemberValues(method, setOf("value"))
            val values = annotationMemberValues.mapNotNull {
                AnnotationUtil.getStringAttributeValue(it)
            }.ifEmpty {
                listOf("")
            }

            val requestMethods = requestMapping.getAnnotationMemberValues(method, setOf("method"))
                .map { it.text.split('.').last() }

            for (value in values) {
                result += EndpointElement(
                    simplifyUrl(prefix, value),
                    requestMethods,
                    method,
                    psiClass,
                    null,
                    EndpointType.SPRING_OPEN_FEIGN
                )
            }
        }

        return result
    }

    private fun simplifyUrl(pathFirst: String, pathSecond: String): String {
        return pathFirst + if (pathSecond.startsWith("/")) pathSecond else "/$pathSecond"
    }
}