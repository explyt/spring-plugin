/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

/**
 * Functional routes of the servlet stack (WebMvc.fn). They are declared with the same DSL and builder as the reactive
 * ones, but a Spring MVC project carries no Reactor, so they cannot be discovered by the WebFlux loader — its
 * applicability check looks for `reactor.core.publisher.Flux`.
 */
class SpringWebMvcFnEndpointsLoader(project: Project) : FunctionalRouteEndpointsLoader(project, EndpointType.SPRING_MVC) {

    override fun isApplicable(module: Module) = SpringWebUtil.isWebMvcFnModule(module)
}
