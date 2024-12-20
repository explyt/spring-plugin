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