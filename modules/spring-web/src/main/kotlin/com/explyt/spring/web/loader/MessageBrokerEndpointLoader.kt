/*
 * Copyright © 2025 Explyt Ltd
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

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.AnnotatedElementsSearch

class MessageBrokerEndpointLoader(private val project: Project) : SpringWebEndpointsLoader {

    override fun isApplicable(module: Module) =
        LibraryClassCache.searchForLibraryClass(project, SpringCoreClasses.MESSAGE_MAPPING) != null
                || LibraryClassCache.searchForLibraryClass(project, SpringCoreClasses.MESSAGE_MAPPING_EE) != null

    override fun getType(): EndpointType {
        return EndpointType.MESSAGE_BROKER
    }

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        val springMessageAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringCoreClasses.MESSAGE_MAPPING, false
        ).takeIf { it.isNotEmpty() } ?: emptyList()

        val eeMessageAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringCoreClasses.MESSAGE_MAPPING_EE, false
        ).takeIf { it.isNotEmpty() } ?: emptyList()

        val allAnnotations = springMessageAnnotations + eeMessageAnnotations

        return allAnnotations.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiMethods(it, module.moduleScope) }
            .map { getEndpoints(it) }
            .toList()
    }

    private fun getEndpoints(psiMethod: PsiMethod): EndpointElement {
        return EndpointElement(
            psiMethod.name,
            listOf(getType().name),
            psiMethod,
            psiMethod.containingClass,
            null,
            getType()
        )
    }
}