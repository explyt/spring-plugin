/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi

import com.explyt.spring.web.builder.AbstractBuilder
import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention
import com.explyt.spring.web.util.SpringWebUtil.simpleTypesMap
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class OpenApiFileBuilder(
    protected val pathsBuilder: OpenApiPathsBuilder,
    protected val componentsBuilder: OpenApiComponentsBuilder,
    protected val serversBuilder: OpenApiServersBuilder,
    builder: StringBuilder
) : AbstractBuilder(builder = builder) {

    /**
     * @param filename - filename without extention
     */
    abstract fun toFile(filename: String, project: Project): PsiFile

    fun addEndpoint(endpoint: AddEndpointToOpenApiIntention.EndpointInfo): OpenApiFileBuilder {
        pathsBuilder.addEndpoint(endpoint)

        addType(endpoint.returnTypeFqn)
        endpoint.requestBodyInfo?.typeFqn?.let { addType(it) }

        for (requestParameter in endpoint.requestParameters) {
            addType(requestParameter.typeFqn)
        }
        for (pathVariable in endpoint.pathVariables) {
            addType(pathVariable.typeFqn)
        }

        return this
    }

    fun addServer(serverUrl: String): OpenApiFileBuilder {
        serversBuilder.addServerUrl(serverUrl)
        return this
    }

    private fun addType(typeCanonical: String) {
        val typeInfo = OpenApiUtils.unwrapType(typeCanonical)
        if (!simpleTypesMap.containsKey(typeInfo.typeQN)) {
            componentsBuilder.addType(typeInfo)
        }
    }

}