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

package com.explyt.spring.web.builder.openapi.yaml

import com.explyt.spring.web.builder.openapi.OpenApiPathBuilder
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo

class OpenApiYamlPathBuilder(
    private val path: String,
    indent: String = "",
    builder: StringBuilder = StringBuilder()
) : OpenApiPathBuilder(indent, builder), YamlKeyValueGenerator {

    override fun build() {
        builder.appendLine()
        builder.append("$indent${path}:")
        for (endpoint in endpointInfos) {
            for (httpType in endpoint.requestMethods) {
                OpenApiYamlPathHttpTypeBuilder(endpoint, httpType, "$indent  ", builder)
                    .build()
            }
        }
    }

    override fun addEndpoint(endpoint: EndpointInfo): OpenApiYamlPathBuilder {
        super.addEndpoint(endpoint)
        return this
    }

}
