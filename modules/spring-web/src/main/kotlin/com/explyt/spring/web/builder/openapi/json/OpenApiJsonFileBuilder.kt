/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.json

import com.explyt.spring.web.builder.openapi.OpenApiFileBuilder
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory

class OpenApiJsonFileBuilder(builder: StringBuilder = StringBuilder()) : OpenApiFileBuilder(
    OpenApiJsonPathsBuilder("  ", builder),
    OpenApiJsonComponentsBuilder("  ", builder),
    OpenApiJsonServersBuilder("  ", builder),
    builder
) {
    override fun toFile(filename: String, project: Project): JsonFile {
        return PsiFileFactory.getInstance(project).createFileFromText(
            "$filename.json",
            JsonFileType.INSTANCE,
            toString()
        ) as JsonFile
    }

    override fun build() {
        builder.append(
            """
            {
              "openapi": "3.1.0",
              "info": {
                "version": "1.1.0",
                "title": "API"
              }
            """.trimIndent()
        )

        serversBuilder.build()
        pathsBuilder.build()
        componentsBuilder.build()

        builder.append("\n}")
    }

}