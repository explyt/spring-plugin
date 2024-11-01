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

package com.explyt.spring.web.service

import com.explyt.spring.web.jsonSchema.OpenApiJsonSchemaReader
import com.explyt.spring.web.model.OpenApiSpecificationType
import com.intellij.json.JsonLanguage
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.containers.ConcurrentFactoryMap
import com.networknt.schema.JsonSchema
import java.util.concurrent.ConcurrentMap


@Service(Service.Level.PROJECT)
class OpenApiSpecificationManager {

    private val schemasBySpecificationType: ConcurrentMap<OpenApiSpecificationType, Pair<VirtualFile, JsonSchema>> =
        ConcurrentFactoryMap.createMap { type -> getSchemasBySpecificationType(type) }

    private fun getSchemasBySpecificationType(specificationType: OpenApiSpecificationType): Pair<VirtualFile, JsonSchema> {
        val schemaFile = getSchemaFile(specificationType)
        val schemaObject = computeSchemaObject(schemaFile)
        return schemaFile to schemaObject
    }

    fun getSchemaFor(specificationType: OpenApiSpecificationType): Pair<VirtualFile, JsonSchema>? {
        return if (specificationType is OpenApiSpecificationType.NONE) null
        else schemasBySpecificationType[specificationType]
    }

    private fun getSchemaFile(specificationType: OpenApiSpecificationType): VirtualFile {
        val resourcePath = getRootSchemaResourcePath(specificationType)
        val inputStream = this.javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: throw RuntimeException("No bundled json schema found in resources folder: '$resourcePath'")

        val defaultSchemaContent = FileUtil.loadTextAndClose(inputStream)

        val jsonFileType = FileTypeManager.getInstance().findFileTypeByLanguage(JsonLanguage.INSTANCE)
        return LightVirtualFile("${specificationType.presentableName}.json", jsonFileType, defaultSchemaContent)
    }

    private fun computeSchemaObject(schemaFile: VirtualFile): JsonSchema =
        runReadAction {
            val jsonSchema = OpenApiJsonSchemaReader.INSTANCE.readFromFile(schemaFile)
                ?: throw AssertionError("Error creates Schema instance from its JSON representation")

            return@runReadAction jsonSchema
        }

    private fun getRootSchemaResourcePath(specificationType: OpenApiSpecificationType): String {
        val resourcePath = when (specificationType) {
            is OpenApiSpecificationType.OpenAPI30Components -> {
                "schema/openapi_3_0_0.json"
            }

            is OpenApiSpecificationType.OpenAPI31Components -> {
                "schema/openapi_3_1_0.json"
            }

            else -> {
                throw java.lang.RuntimeException("No schema file exist for specification type: $specificationType")
            }
        }
        return resourcePath
    }
}