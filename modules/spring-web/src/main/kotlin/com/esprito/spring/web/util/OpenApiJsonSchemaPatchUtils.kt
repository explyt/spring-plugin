package com.esprito.spring.web.util

import com.esprito.spring.web.model.OpenApiSpecificationType
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException

class OpenApiJsonSchemaPatchUtils {

    fun applySuitablePatches(jsonSchemaContent: String, specificationType: OpenApiSpecificationType): String {
        if (jsonSchemaContent.isEmpty()) return jsonSchemaContent

        val schemaRootNode = readTree(jsonSchemaContent) ?: return jsonSchemaContent
        val mutableTransformedSchema = transformSchemaBeforePatching(schemaRootNode)

        val allPatches = findAllSuitablePatches(specificationType)

        val allStaticPatchesApplied = allPatches.all { patch ->
            INSTANCE.mergePatchIntoSchema(mutableTransformedSchema, patch)
        }

        return if (allStaticPatchesApplied) {
            asPrettyString(mutableTransformedSchema)
        } else {
            Logger.getInstance(OpenApiJsonSchemaPatchUtils::class.java).warn(
                "Some patches were applied with errors, default schema for specification type $specificationType will be used"
            )
            jsonSchemaContent
        }
    }

    private fun findAllSuitablePatches(specificationType: OpenApiSpecificationType): Sequence<VirtualFile> {
        val patchNamePrefix = buildString {
            when (specificationType) {
                is OpenApiSpecificationType.OpenAPI30Family -> append("3_0_0")
                is OpenApiSpecificationType.OpenAPI31Family -> append("3_1_0")
                else -> return emptySequence()
            }
        }

        return runReadAction {
            val resourceDir = getBundledResource("schema/patches")
            val files = resourceDir?.children.orEmpty()
            files.asSequence().filter { it.nameWithoutExtension.startsWith(patchNamePrefix) }
        }
    }

    private fun mergePatchIntoSchema(schemaRootNode: JsonNode, patchVirtualFile: VirtualFile): Boolean {
        return try {
            val patchContent = patchVirtualFile.inputStream.use { FileUtil.loadTextAndClose(it) }
            val patchRootNode = readTree(patchContent) ?: return false
            mergeTrees(schemaRootNode, patchRootNode)

            true
        } catch (ex: IOException) {
            Logger.getInstance(OpenApiJsonSchemaPatchUtils::class.java)
                .warn("Unable to apply patch ${patchVirtualFile.name} to json schema", ex)

            false
        }
    }

    private fun mergeTrees(targetSchema: JsonNode, patch: JsonNode): JsonNode {
        if (targetSchema is ObjectNode) {
            patch.fields().forEach { (key, value) ->
                val keyWithoutPrefixes = key.removePrefix("~").removePrefix("!")

                when {
                    key.startsWith("!") -> return@forEach

                    targetSchema.has(keyWithoutPrefixes) && targetSchema[keyWithoutPrefixes] !is TextNode -> {
                        if (key.startsWith("~")) {
                            targetSchema.remove(keyWithoutPrefixes)
                        } else {
                            mergeTrees(targetSchema[keyWithoutPrefixes], value)
                        }
                    }

                    else -> targetSchema.set<JsonNode>(keyWithoutPrefixes, value)
                }
            }
        } else if (targetSchema is ArrayNode) {
            when (patch) {
                is ArrayNode -> mergeArrays(targetSchema, patch)
                is ObjectNode -> addItemToArray(targetSchema, patch)
            }
        }
        return targetSchema
    }

    private fun readTree(jsonSchemaContent: String): JsonNode? {
        return try {
            mapper.readTree(jsonSchemaContent)
        } catch (exception: IOException) {
            Logger.getInstance(OpenApiJsonSchemaPatchUtils::class.java)
                .warn("Unable to read json schema from provided file", exception)
            null
        }
    }

    private fun mergeArrays(targetArray: ArrayNode, patchArray: ArrayNode) {
        patchArray.elements().forEach { arrayItem ->
            if (arrayItem is TextNode && !hasTextNode(targetArray, arrayItem)) {
                addItemToArray(targetArray, arrayItem)
            }
        }
    }

    private fun addItemToArray(targetArray: ArrayNode, newItem: JsonNode) {
        targetArray.add(newItem)
    }

    private fun hasTextNode(arrayNode: ArrayNode, textNode: TextNode): Boolean {
        return arrayNode.asSequence()
            .filterIsInstance<TextNode>()
            .any { it.textValue() == textNode.textValue() }
    }

    private fun transformSchemaBeforePatching(schemaRootNode: JsonNode): JsonNode {
        dfsProcessTree(schemaRootNode)
        return schemaRootNode
    }

    private fun dfsProcessTree(node: JsonNode) {
        if (node is ObjectNode) {
            for ((key, value) in node.fields()) {
                if (replaceIfElseWithOneOf(node, key as String)) {
                    return
                }
                dfsProcessTree(value as JsonNode)
            }
        }

    }

    private fun replaceIfElseWithOneOf(parentNode: ObjectNode, key: String): Boolean {
        val keywords = setOf("if", "then", "else")

        if (key !in keywords) return false

        val thenBranchBody = parentNode["then"] ?: return true
        val elseBranchBody = parentNode["else"] ?: return true

        val arrayNode = JsonNodeFactory.instance.arrayNode().add(thenBranchBody).add(elseBranchBody)

        parentNode.set<JsonNode>("oneOf", arrayNode)
        parentNode.remove(listOf("if", "then", "else"))

        return true
    }

    private fun getBundledResource(resourceName: String): VirtualFile? {
        val classLoader = this::class.java.classLoader
        val resourceUrl = classLoader.getResource(resourceName) ?: return null
        return VfsUtil.findFileByURL(resourceUrl)
    }

    private fun asPrettyString(jsonNode: JsonNode): String {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode)
    }

    companion object {
        val INSTANCE: OpenApiJsonSchemaPatchUtils = OpenApiJsonSchemaPatchUtils()
        val mapper: ObjectMapper = ObjectMapper(JsonFactory())
    }
}

