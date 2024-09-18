package com.esprito.spring.web.service

import com.esprito.spring.web.model.OpenApiSpecificationType
import com.intellij.json.JsonLanguage
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.containers.ConcurrentFactoryMap
import com.jetbrains.jsonSchema.impl.JsonSchemaObject
import com.jetbrains.jsonSchema.impl.JsonSchemaReader
import java.util.concurrent.ConcurrentMap


@Service(Service.Level.PROJECT)
class OpenApiSpecificationManager(private val project: Project) {

    private val schemasBySpecificationType: ConcurrentMap<OpenApiSpecificationType, Pair<VirtualFile, JsonSchemaObject>> =
        ConcurrentFactoryMap.createMap { type -> getSchemasBySpecificationType(type) }

    private fun getSchemasBySpecificationType(specificationType: OpenApiSpecificationType): Pair<VirtualFile, JsonSchemaObject> {
        val schemaFile = getSchemaFile(specificationType)
        val schemaObject = computeSchemaObject(schemaFile, project)
        return schemaFile to schemaObject
    }

    fun getSchemaFor(specificationType: OpenApiSpecificationType): Pair<VirtualFile, JsonSchemaObject>? {
        return if (specificationType is OpenApiSpecificationType.NONE) null
        else schemasBySpecificationType[specificationType]
    }

    private fun getSchemaFile(specificationType: OpenApiSpecificationType): VirtualFile {
        val resourcePath = getRootSchemaResourcePath(specificationType)
        val inputStream = this.javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: throw RuntimeException("No bundled json schema found in resources folder: '$resourcePath'")

        val defaultSchemaContent = FileUtil.loadTextAndClose(inputStream)

//        val patchedSchemaContent = OpenApiJsonSchemaPatchUtils.INSTANCE.applySuitablePatches(defaultSchemaContent, specificationType)

        val jsonFileType = FileTypeManager.getInstance().findFileTypeByLanguage(JsonLanguage.INSTANCE)
        return LightVirtualFile("${specificationType.presentableName}.json", jsonFileType, defaultSchemaContent)
    }

    private fun computeSchemaObject(schemaFile: VirtualFile, project: Project): JsonSchemaObject =
        runReadAction {
            val schemaPsiFile = PsiManager.getInstance(project).findFile(schemaFile)
                ?: throw AssertionError("Error created PSI schema file")

            //FIXME: rewrite schema loading
            @Suppress("DEPRECATION")
            val schemaObject = JsonSchemaReader(schemaFile).read(schemaPsiFile)
                ?: throw AssertionError("Error creates Schema instance from its JSON representation")

            return@runReadAction schemaObject
        }

    private fun getRootSchemaResourcePath(specificationType: OpenApiSpecificationType): String {
        val resourcePath = when (specificationType) {
            is OpenApiSpecificationType.OpenAPI30Family -> {
                "schema/openapi_3_0_0.json"
            }

            is OpenApiSpecificationType.OpenAPI31Family -> {
                "schema/openapi_3_1_0.json"
            }

            else -> {
                throw java.lang.RuntimeException("No schema file exist for specification type: $specificationType")
            }
        }
        return resourcePath
    }
}