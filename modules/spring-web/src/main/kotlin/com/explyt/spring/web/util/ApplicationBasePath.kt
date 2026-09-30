/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.core.properties.FoldedPropertyValue
import com.explyt.spring.web.SpringWebClasses
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade

/**
 * The path a Spring Boot application puts in front of every mapping, as its configuration declares it.
 *
 * A servlet application serves under `server.servlet.context-path` and then under the dispatcher servlet's
 * `spring.mvc.servlet.path`; a reactive one under `spring.webflux.base-path`. Which of them applies is decided by the
 * web stack on the module's classpath - a key of the other stack is ignored by Boot, and reading it would put a
 * prefix in front of URLs the application does not serve.
 *
 * `null` when nothing is declared: a request URL may still carry a prefix that lives in deployment configuration
 * only, which is a different, unverifiable fact.
 */
object ApplicationBasePath {

    fun of(module: Module): String? {
        val keys = when (stackOf(module)) {
            WebStack.SERVLET -> SERVLET_KEYS
            WebStack.REACTIVE -> REACTIVE_KEYS
            null -> return null
        }
        return keys.asSequence()
            .mapNotNull { key -> FoldedPropertyValue.resolve(module, key)?.value }
            .map { MappingPathPlaceholders.resolve(module, it).trim() }
            .filter { it.isNotEmpty() && it != "/" && MappingPathPlaceholders.PLACEHOLDER_START !in it }
            .joinToString("") { "/" + it.trim('/') }
            .ifEmpty { null }
            ?.let(SpringWebUtil::simplifyUrl)
    }

    private fun stackOf(module: Module): WebStack? {
        val facade = JavaPsiFacade.getInstance(module.project)
        val scope = module.moduleWithLibrariesScope
        return when {
            facade.findClass(SpringWebClasses.MVC_DISPATCHER_SERVLET, scope) != null -> WebStack.SERVLET
            facade.findClass(SpringWebClasses.WEBFLUX_DISPATCHER_HANDLER, scope) != null -> WebStack.REACTIVE
            else -> null
        }
    }

    private enum class WebStack { SERVLET, REACTIVE }

    private val SERVLET_KEYS = listOf("server.servlet.context-path", "spring.mvc.servlet.path")
    private val REACTIVE_KEYS = listOf("spring.webflux.base-path")
}
