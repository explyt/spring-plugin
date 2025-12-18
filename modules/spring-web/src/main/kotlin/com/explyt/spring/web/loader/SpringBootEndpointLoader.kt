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