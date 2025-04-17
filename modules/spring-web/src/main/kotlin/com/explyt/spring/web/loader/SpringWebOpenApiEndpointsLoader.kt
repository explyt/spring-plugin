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