/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiMember

/**
 * see org.springframework.boot.autoconfigure.condition.OnWebApplicationCondition
 */
class OnWebApplicationConditionStrategy(val module: Module) : AnnotationConditionStrategy(
    listOf(
        SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_WEB_APPLICATION)
    ),
    setOf(ConditionAssumption.COMPILE_CLASSPATH_IS_RUNTIME)
) {
    private val SERVLET_WEB_APPLICATION_CLASS: String =
        "org.springframework.web.context.support.GenericWebApplicationContext"
    private val REACTIVE_WEB_APPLICATION_CLASS: String = "org.springframework.web.reactive.HandlerResult"

    override fun unmetRequirement(
        holder: MetaAnnotationsHolder, carrier: PsiMember, activeBeans: Collection<PsiBean>
    ): String? {
        val webType = holder.getAnnotationMemberValues(carrier, setOf("type"))
            .asSequence()
            .mapNotNull { getWebType(it) }
            .firstOrNull() ?: WebType.ANY
        val hasReactive = { LibraryClassCache.searchForLibraryClass(module, REACTIVE_WEB_APPLICATION_CLASS) != null }
        val hasServlet = { LibraryClassCache.searchForLibraryClass(module, SERVLET_WEB_APPLICATION_CLASS) != null }
        val matches = when (webType) {
            WebType.REACTIVE -> hasReactive()
            WebType.SERVLET -> hasServlet()
            WebType.ANY -> hasServlet() || hasReactive()
        }
        return if (matches) null else "no ${webType.name.lowercase()} web application classes on the classpath"
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
