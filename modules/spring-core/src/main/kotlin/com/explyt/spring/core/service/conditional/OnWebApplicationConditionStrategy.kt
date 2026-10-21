/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.util.BootWebApplicationType
import com.explyt.spring.core.util.RuntimeClasspath
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember
import org.jetbrains.uast.UExpression

/**
 * see org.springframework.boot.autoconfigure.condition.OnWebApplicationCondition
 */
internal class OnWebApplicationConditionStrategy(module: Module) :
    WebApplicationTypeConditionStrategy(module, SpringCoreClasses.CONDITIONAL_ON_WEB_APPLICATION) {

    override fun unmetRequirement(
        holder: MetaAnnotationsHolder, carrier: PsiMember, activeBeans: Collection<PsiBean>
    ): String? {
        val webType = holder.mergedValuesOf(carrier, setOf("type"))
            .firstNotNullOfOrNull { webTypeOf(it) } ?: WebType.ANY
        return if (web.matches(webType)) null
        else "the application does not run as a ${webType.name.lowercase()} web application"
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

internal class OnNotWebApplicationConditionStrategy(module: Module) :
    WebApplicationTypeConditionStrategy(module, SpringCoreClasses.CONDITIONAL_ON_NOT_WEB_APPLICATION) {

    override fun unmetRequirement(
        holder: MetaAnnotationsHolder, carrier: PsiMember, activeBeans: Collection<PsiBean>
    ): String? = if (web.matches(WebType.ANY)) "the application runs as a web application" else null
}

internal abstract class WebApplicationTypeConditionStrategy(protected val module: Module, private val annotationFqn: String) :
    AnnotationConditionStrategy(
        listOf(SpringSearchService.getInstance(module.project).getMetaAnnotations(module, annotationFqn)),
        setOf(ConditionAssumption.COMPILE_CLASSPATH_IS_RUNTIME)
    ) {

    protected val web by lazy { WebApplication(module) }

    override fun verdictOf(carrier: PsiMember, activeBeans: Collection<PsiBean>): ConditionVerdict {
        if (web.type != BootWebApplicationType.UNKNOWN) return super.verdictOf(carrier, activeBeans)
        val annotation = carrier.annotations.firstOrNull { handles(it) } ?: return ConditionVerdict.Active
        return ConditionVerdict.Undecided(
            listOf(
                ConditionEvidence(
                    annotationFqn = annotation.qualifiedName ?: annotationFqn,
                    carrierFqn = carrier.conditionCarrierFqn,
                    detail = "${BootWebApplicationType.WEB_APPLICATION_TYPE_KEY} cannot be resolved",
                    reason = ConditionReason.PROPERTY_UNRESOLVABLE,
                    assumptions = setOf(ConditionAssumption.COMPILE_CLASSPATH_IS_RUNTIME)
                )
            )
        )
    }
}

internal class WebApplication(module: Module) {
    val type: BootWebApplicationType = BootWebApplicationType.cachedOf(module)
    private val runtimeClasspath by lazy { RuntimeClasspath(module) }

    fun matches(webType: WebType): Boolean = when (webType) {
        WebType.SERVLET -> isServlet
        WebType.REACTIVE -> isReactive
        WebType.ANY -> isServlet || isReactive
    }

    private val isServlet: Boolean by lazy {
        type == BootWebApplicationType.SERVLET && runtimeClasspath.has(SERVLET_WEB_APPLICATION_CLASS)
    }

    private val isReactive: Boolean by lazy {
        type == BootWebApplicationType.REACTIVE && runtimeClasspath.has(REACTIVE_WEB_APPLICATION_CLASS)
    }

    private companion object {
        const val SERVLET_WEB_APPLICATION_CLASS = "org.springframework.web.context.support.GenericWebApplicationContext"
        const val REACTIVE_WEB_APPLICATION_CLASS = "org.springframework.web.reactive.HandlerResult"
    }
}

internal enum class WebType {
    ANY, SERVLET, REACTIVE
}
