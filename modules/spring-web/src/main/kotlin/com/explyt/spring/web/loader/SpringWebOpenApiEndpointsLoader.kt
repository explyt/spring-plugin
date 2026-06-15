/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiJsonData
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiYamlData
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project


class SpringWebOpenApiEndpointsLoader(private val project: Project) : SpringWebEndpointsLoader {

    override fun isApplicable(module: Module) = true

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        return (findOpenApiJsonData(project).asSequence() + findOpenApiYamlData(project).asSequence())
            .filter { module == ModuleUtilCore.findModuleForPsiElement(it.psiFile) }
            .flatMap { it.endpoints }
            .filterIsInstance<EndpointData.EndpointElementData>()
            .map { it.endpointElement }
            .toList()
    }

    override fun getType(): EndpointType {
        return EndpointType.OPENAPI
    }
}