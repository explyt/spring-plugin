/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
            OpenApiJsonSchemaProvider(
                project,
                OpenApiSpecificationType.OpenApiV30.INSTANCE,
                SpringWebBundle.message("explyt.openapi.3.0.schema.name"),
                SpringWebBundle.message("explyt.openapi.3.0.schema.remote.url")
            ),
            OpenApiJsonSchemaProvider(
                project,
                OpenApiSpecificationType.OpenApiV31.INSTANCE,
                SpringWebBundle.message("explyt.openapi.3.1.schema.name"),
                SpringWebBundle.message("explyt.openapi.3.1.schema.remote.url")
            ),
        )
    }
}