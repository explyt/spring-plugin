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