/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.AnnotatedElementsSearch

class EventListenersEndpointLoader(private val project: Project) : SpringWebEndpointsLoader {

    override fun isApplicable(module: Module) =
        LibraryClassCache.searchForLibraryClass(project, SpringCoreClasses.EVENT_LISTENER) != null

    override fun getType(): EndpointType {
        return EndpointType.EVENT_LISTENERS
    }

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        val springMessageAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringCoreClasses.EVENT_LISTENER, false
        ).takeIf { it.isNotEmpty() } ?: emptyList()

        return springMessageAnnotations.asSequence()
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