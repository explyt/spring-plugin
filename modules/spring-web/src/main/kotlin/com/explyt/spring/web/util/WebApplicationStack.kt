/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.web.SpringWebClasses
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade

/**
 * The web stack a Spring Boot application of a module runs on, decided the way `WebApplicationType.deduce()` does.
 *
 * A module carrying both stacks is a servlet application: Boot picks servlet MVC whenever `DispatcherServlet` is on
 * the classpath, and a project usually adds WebFlux next to it only for `WebClient`. Only a classpath with
 * `DispatcherHandler` and no `DispatcherServlet` runs reactive.
 *
 * The `spring.main.web-application-type` override is not read.
 */
enum class WebApplicationStack {
    SERVLET, REACTIVE;

    companion object {
        /** The stack of [module], or `null` when neither dispatcher is on its classpath. */
        fun of(module: Module): WebApplicationStack? {
            val facade = JavaPsiFacade.getInstance(module.project)
            val scope = module.moduleWithLibrariesScope
            return when {
                facade.findClass(SpringWebClasses.MVC_DISPATCHER_SERVLET, scope) != null -> SERVLET
                facade.findClass(SpringWebClasses.WEBFLUX_DISPATCHER_HANDLER, scope) != null -> REACTIVE
                else -> null
            }
        }
    }
}
