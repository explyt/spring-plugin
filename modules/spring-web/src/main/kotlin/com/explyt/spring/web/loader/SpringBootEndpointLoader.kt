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
import com.intellij.psi.PsiClass
import com.intellij.psi.search.searches.AnnotatedElementsSearch

class SpringBootEndpointLoader(private val project: Project) : SpringWebEndpointsLoader {

    override fun isApplicable(module: Module) =
        LibraryClassCache.searchForLibraryClass(project, SpringCoreClasses.SPRING_BOOT_APPLICATION) != null

    override fun getType(): EndpointType {
        return EndpointType.SPRING_BOOT
    }

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        val springBootAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringCoreClasses.SPRING_BOOT_APPLICATION, false
        ).takeIf { it.isNotEmpty() } ?: emptyList()

        return springBootAnnotations.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, module.moduleScope) }
            .mapNotNull { getEndpoints(it) }
            .toList()
    }

    private fun getEndpoints(psiClass: PsiClass): EndpointElement? {
        val className = psiClass.name ?: return null
        return EndpointElement(
            className,
            listOf(getType().name),
            psiClass,
            psiClass,
            null,
            getType()
        )
    }
}