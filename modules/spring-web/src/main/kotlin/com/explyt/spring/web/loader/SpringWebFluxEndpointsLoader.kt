/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

class SpringWebFluxEndpointsLoader(project: Project) :
    FunctionalRouteEndpointsLoader(project, EndpointType.SPRING_WEBFLUX) {

    override fun isApplicable(module: Module) = SpringWebUtil.isFluxWebModule(module)
}
