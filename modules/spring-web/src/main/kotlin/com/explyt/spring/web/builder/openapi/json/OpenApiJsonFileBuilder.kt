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