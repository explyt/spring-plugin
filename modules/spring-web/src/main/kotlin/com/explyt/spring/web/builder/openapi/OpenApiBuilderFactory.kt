/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi

import com.explyt.spring.web.builder.openapi.json.OpenApiJsonFileBuilder
import com.explyt.spring.web.builder.openapi.yaml.OpenApiYamlFileBuilder
import com.intellij.json.JsonFileType
import com.intellij.openapi.fileTypes.FileType
import org.jetbrains.yaml.YAMLFileType

object OpenApiBuilderFactory {

    fun getOpenApiFileBuilder(type: FileType): OpenApiFileBuilder {
        return when (type) {
            is JsonFileType -> OpenApiJsonFileBuilder()
            is YAMLFileType -> OpenApiYamlFileBuilder()

            else -> throw UnknownFileTypeException(type)
        }
    }

    class UnknownFileTypeException(type: FileType) : RuntimeException("${type.name} is not supported")

}