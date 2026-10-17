/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.explyt.spring.core.service.conditional.ConditionPropertyValue
import com.explyt.spring.core.service.conditional.PropertyConditionValues
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.util.CacheUtils
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade

enum class BootWebApplicationType {
    SERVLET, REACTIVE, NONE, UNKNOWN;

    companion object {
        const val WEB_APPLICATION_TYPE_KEY = "spring.main.web-application-type"

        fun of(module: Module): BootWebApplicationType = when (val declared = declaredOf(module)) {
            null -> RuntimeClasspath(module).deduced(servletApis = servletApisOf(module))
            else -> declared
        }

        fun cachedOf(module: Module): BootWebApplicationType =
            CacheUtils.getCachedValue(
                module, ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            ) { of(module) }

        private fun declaredOf(module: Module): BootWebApplicationType? =
            when (val value = PropertyConditionValues(module).valueOf(WEB_APPLICATION_TYPE_KEY)) {
                ConditionPropertyValue.Missing -> null
                ConditionPropertyValue.Unresolvable -> UNKNOWN
                is ConditionPropertyValue.Known -> DECLARABLE.firstOrNull { it.name.equals(value.text.trim(), true) }
                    ?: UNKNOWN
            }

        private fun servletApisOf(module: Module): List<String> =
            if (SpringBootUtil.isBelowSpringBoot3(module)) listOf(SERVLET_API, JAVAX_SERVLET_API) else listOf(SERVLET_API)

        private fun RuntimeClasspath.deduced(servletApis: List<String>): BootWebApplicationType =
            if (has(BOOT_4_DEDUCER)) deducedByBoot4() else deducedByBoot3(servletApis)

        private fun RuntimeClasspath.deducedByBoot3(servletApis: List<String>): BootWebApplicationType = when {
            has(DISPATCHER_HANDLER) && !has(DISPATCHER_SERVLET) && !has(JERSEY_SERVLET_CONTAINER) -> REACTIVE
            has(CONFIGURABLE_WEB_CONTEXT) && servletApis.any { has(it) } -> SERVLET
            else -> NONE
        }

        private fun RuntimeClasspath.deducedByBoot4(): BootWebApplicationType = when {
            has(BOOT_4_WEBMVC_DEDUCER) && WEBMVC_INDICATORS.all { has(it) } -> SERVLET
            has(BOOT_4_WEBFLUX_DEDUCER) && WEBFLUX_INDICATORS.all { has(it) } -> REACTIVE
            SERVLET_INDICATORS.all { has(it) } -> SERVLET
            else -> NONE
        }

        private val DECLARABLE = listOf(SERVLET, REACTIVE, NONE)

        private const val BOOT_4_DEDUCER = "org.springframework.boot.WebApplicationType.Deducer"
        private const val BOOT_4_WEBMVC_DEDUCER = "org.springframework.boot.webmvc.WebMvcWebApplicationTypeDeducer"
        private const val BOOT_4_WEBFLUX_DEDUCER = "org.springframework.boot.webflux.WebFluxWebApplicationTypeDeducer"
        private const val DISPATCHER_SERVLET = "org.springframework.web.servlet.DispatcherServlet"
        private const val DISPATCHER_HANDLER = "org.springframework.web.reactive.DispatcherHandler"
        private const val JERSEY_SERVLET_CONTAINER = "org.glassfish.jersey.servlet.ServletContainer"
        private const val SERVLET_API = "jakarta.servlet.Servlet"
        private const val JAVAX_SERVLET_API = "javax.servlet.Servlet"
        private const val CONFIGURABLE_WEB_CONTEXT = "org.springframework.web.context.ConfigurableWebApplicationContext"
        private const val REACTOR_MONO = "reactor.core.publisher.Mono"

        private val SERVLET_INDICATORS = listOf(SERVLET_API, CONFIGURABLE_WEB_CONTEXT)
        private val WEBMVC_INDICATORS = listOf(SERVLET_API, DISPATCHER_SERVLET, CONFIGURABLE_WEB_CONTEXT)
        private val WEBFLUX_INDICATORS = listOf(REACTOR_MONO, DISPATCHER_HANDLER)
    }
}

internal class RuntimeClasspath(module: Module) {
    private val facade = JavaPsiFacade.getInstance(module.project)
    private val scope = module.getModuleRuntimeScope(false)
    private val known = HashMap<String, Boolean>()

    fun has(fqn: String): Boolean = known.getOrPut(fqn) { facade.findClass(fqn, scope) != null }
}
