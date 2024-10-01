package com.esprito.spring.web.jsonSchema

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

