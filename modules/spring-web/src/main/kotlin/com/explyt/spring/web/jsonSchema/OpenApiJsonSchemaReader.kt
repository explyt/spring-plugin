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

package com.explyt.spring.web.jsonSchema

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.vfs.VirtualFile
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion


class OpenApiJsonSchemaReader {

    fun readFromFile(file: VirtualFile): JsonSchema? {
        val jsonSchemaFactory = when (getDraftVersion(file)) {
            DraftVersion.DRAFT_04 -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
            DraftVersion.DRAFT_2020_12 -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
            else -> null
        }

        return jsonSchemaFactory?.getSchema(file.inputStream)
    }

    private fun getDraftVersion(file: VirtualFile): DraftVersion {
        val jsonNode = mapper.readTree(file.inputStream)
        val schemaUrl = jsonNode["\$schema"].asText()
        val matchResult = draftVersionRegex.find(schemaUrl)
        val draftVersion = matchResult?.groups?.get(1)?.value ?: ""
        return DraftVersion.fromValue(draftVersion)
    }

    companion object {
        val INSTANCE: OpenApiJsonSchemaReader = OpenApiJsonSchemaReader()
        val mapper: ObjectMapper = ObjectMapper(JsonFactory())

        val draftVersionRegex = Regex("""draft[-/]([\d-]+)""")
    }

    enum class DraftVersion(val value: String) {
        DRAFT_04("04"),
        DRAFT_2020_12("2020-12"),
        NONE("");

        companion object {
            fun fromValue(value: String): DraftVersion {
                return entries.find { it.value == value } ?: NONE
            }
        }
    }
}

