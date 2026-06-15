/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.yaml

import com.explyt.spring.web.builder.openapi.OpenApiFileBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile

class OpenApiYamlFileBuilder(builder: StringBuilder = StringBuilder()) : OpenApiFileBuilder(
    OpenApiYamlPathsBuilder("", builder),
    OpenApiYamlComponentsBuilder("", builder),
    OpenApiYamlServersBuilder("", builder),
    builder
) {
    override fun toFile(filename: String, project: Project): YAMLFile {
        return PsiFileFactory.getInstance(project).createFileFromText(
            "$filename.yml",
            YAMLFileType.YML,
            toString()
        ) as YAMLFile
    }

    override fun build() {
        builder.append(
            """
            openapi: 3.1.0
            info:
              version: 1.0.0
              title: API
        """.trimIndent()
        )

        serversBuilder.build()
        pathsBuilder.build()
        componentsBuilder.build()
    }

}