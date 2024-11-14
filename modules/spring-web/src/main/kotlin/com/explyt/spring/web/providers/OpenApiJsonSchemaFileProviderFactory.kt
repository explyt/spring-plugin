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

package com.explyt.spring.web.providers

import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.model.OpenApiSpecificationType
import com.intellij.openapi.project.Project
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory

class OpenApiJsonSchemaFileProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project): MutableList<JsonSchemaFileProvider> {
        return mutableListOf(
            SpecificationJsonSchemaFileProvider(
                project,
                OpenApiSpecificationType.OpenApiV30.INSTANCE,
                SpringWebBundle.message("explyt.openapi.3.0.schema.name"),
                SpringWebBundle.message("explyt.openapi.3.0.schema.remote.url")
            ),
            SpecificationJsonSchemaFileProvider(
                project,
                OpenApiSpecificationType.OpenApiV31.INSTANCE,
                SpringWebBundle.message("explyt.openapi.3.1.schema.name"),
                SpringWebBundle.message("explyt.openapi.3.1.schema.remote.url")
            ),
        )
    }
}