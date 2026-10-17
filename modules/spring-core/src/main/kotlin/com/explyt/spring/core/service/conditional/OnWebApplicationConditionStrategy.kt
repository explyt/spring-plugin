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
import com.intellij.psi.PsiMember
import org.jetbrains.uast.UExpression

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
        val webType = holder.mergedValuesOf(carrier, setOf("type"))
            .firstNotNullOfOrNull { webTypeOf(it) } ?: WebType.ANY
        val hasReactive = { LibraryClassCache.searchForLibraryClass(module, REACTIVE_WEB_APPLICATION_CLASS) != null }
        val hasServlet = { LibraryClassCache.searchForLibraryClass(module, SERVLET_WEB_APPLICATION_CLASS) != null }
        val matches = when (webType) {
            WebType.REACTIVE -> hasReactive()
            WebType.SERVLET -> hasServlet()
            WebType.ANY -> hasServlet() || hasReactive()
        }
        return if (matches) null else "no ${webType.name.lowercase()} web application classes on the classpath"
    }

    private fun webTypeOf(value: UExpression): WebType? {
        value.enumConstantName?.let { name -> return SPECIFIC_WEB_TYPES.firstOrNull { it.name == name } }
        val text = value.sourcePsi?.text?.lowercase() ?: return null
        return SPECIFIC_WEB_TYPES.firstOrNull { it.name.lowercase() in text }
    }

    private companion object {
        val SPECIFIC_WEB_TYPES = listOf(WebType.REACTIVE, WebType.SERVLET)
    }
}

internal enum class WebType {
    ANY, SERVLET, REACTIVE
}
