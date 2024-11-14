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
import com.networknt.schema.JsonSchema
import java.util.concurrent.ConcurrentHashMap


@Service(Service.Level.PROJECT)
class OpenApiSpecificationManager {

    private val schemas: MutableMap<OpenApiSpecificationType, FileSchemaPair?> = ConcurrentHashMap()

    fun getSchemaByFile(specificationType: OpenApiSpecificationType): FileSchemaPair? {
        return if (specificationType is OpenApiSpecificationType.OpenApiUndefined) null
        else schemas.computeIfAbsent(specificationType) {
            getSchemasByType(it)
        }
    }

    private fun getSchemasByType(specificationType: OpenApiSpecificationType): FileSchemaPair? {
        val schemaFile = getSchemaFileByType(specificationType) ?: return null
        val schemaObject = getSchemaObject(schemaFile) ?: return null
        return FileSchemaPair(schemaFile, schemaObject)
    }

    private fun getSchemaObject(schemaFile: VirtualFile): JsonSchema? =
        runReadAction {
            val jsonSchema = OpenApiJsonSchemaReader.INSTANCE.readFromFile(schemaFile) ?: return@runReadAction null
            return@runReadAction jsonSchema
        }

    private fun getSchemaFileByType(specificationType: OpenApiSpecificationType): VirtualFile? {
        val schemaPath = getSchemaPath(specificationType) ?: return null
        val inputStream = this.javaClass.classLoader.getResourceAsStream(schemaPath) ?: return null
        val schemaText = FileUtil.loadTextAndClose(inputStream)

        val jsonFileType = FileTypeManager.getInstance().findFileTypeByLanguage(JsonLanguage.INSTANCE)
        return LightVirtualFile("${specificationType.name}.json", jsonFileType, schemaText)
    }

    private fun getSchemaPath(specificationType: OpenApiSpecificationType): String? {
        return when (specificationType) {
            is OpenApiSpecificationType.OpenApiV30 -> "schema/openapi_3_0_0.json"
            is OpenApiSpecificationType.OpenApiV31 -> "schema/openapi_3_1_0.json"
            else -> null
        }
    }

    data class FileSchemaPair(
        val file: VirtualFile,
        val schema: JsonSchema
    )
}