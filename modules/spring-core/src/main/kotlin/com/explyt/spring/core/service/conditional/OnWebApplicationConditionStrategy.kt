/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiMember

/**
 * see org.springframework.boot.autoconfigure.condition.OnWebApplicationCondition
 */
class OnWebApplicationConditionStrategy(val module: Module) : ExclusionStrategy {
    private val SERVLET_WEB_APPLICATION_CLASS: String =
        "org.springframework.web.context.support.GenericWebApplicationContext"
    private val REACTIVE_WEB_APPLICATION_CLASS: String = "org.springframework.web.reactive.HandlerResult"

    private val annotationHolder = SpringSearchService.getInstance(module.project)
        .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_WEB_APPLICATION)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
        if (dependant.annotations.none { annotationHolder.contains(it) }) {
            return false
        }

        val webType = annotationHolder.getAnnotationMemberValues(dependant, setOf("type"))
            .asSequence()
            .mapNotNull { getWebType(it) }
            .firstOrNull() ?: WebType.ANY
        return when (webType) {
            WebType.REACTIVE -> {
                val reactorClass = LibraryClassCache.searchForLibraryClass(module, REACTIVE_WEB_APPLICATION_CLASS)
                reactorClass == null
            }

            WebType.SERVLET -> {
                val servletClass = LibraryClassCache.searchForLibraryClass(module, SERVLET_WEB_APPLICATION_CLASS)
                servletClass == null
            }

            else -> {
                val reactorClass = LibraryClassCache.searchForLibraryClass(module, REACTIVE_WEB_APPLICATION_CLASS)
                val servletClass = LibraryClassCache.searchForLibraryClass(module, SERVLET_WEB_APPLICATION_CLASS)
                servletClass == null && reactorClass == null
            }
        }
    }

    private fun getWebType(it: PsiAnnotationMemberValue): WebType? {
        val lowercaseTypeString = it.text?.lowercase()
        return if (lowercaseTypeString?.contains(WebType.REACTIVE.name.lowercase()) == true) {
            WebType.REACTIVE
        } else if (lowercaseTypeString?.contains(WebType.SERVLET.name.lowercase()) == true) {
            WebType.SERVLET
        } else null
    }
}

internal enum class WebType {
    ANY, SERVLET, REACTIVE
}