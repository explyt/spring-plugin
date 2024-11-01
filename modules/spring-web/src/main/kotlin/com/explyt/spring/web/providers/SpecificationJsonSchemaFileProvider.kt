package com.explyt.spring.web.providers

import com.explyt.spring.web.model.OpenApiSpecificationDetection
import com.explyt.spring.web.model.OpenApiSpecificationType
import com.explyt.spring.web.service.OpenApiSpecificationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.SchemaType

class SpecificationJsonSchemaFileProvider(
    private val specificationType: OpenApiSpecificationType,
    private val remoteSchemaUrl: String,
    private val visibleName: String,
    private val project: Project
) : JsonSchemaFileProvider {

    override fun isAvailable(file: VirtualFile): Boolean {
        val detectedSpecificationType: OpenApiSpecificationType = runReadAction<Any> label@{
            if (!file.isValid) {
                return@label OpenApiSpecificationType.UNKNOWN.INSTANCE
            } else {
                val psiFile = PsiManager.getInstance(project).findFile(file)
                if (psiFile == null) {
                    return@label OpenApiSpecificationType.UNKNOWN.INSTANCE
                } else {
                    return@label OpenApiSpecificationDetection.detectPrimarySpecificationType(file, psiFile)
                }
            }
        } as OpenApiSpecificationType
        return detectedSpecificationType == specificationType
    }

    override fun getName(): String {
        return visibleName
    }

    override fun getSchemaFile(): VirtualFile? {
        val schemaStorage = project.getService(OpenApiSpecificationManager::class.java)

        return schemaStorage?.getSchemaFor(specificationType)?.first
    }

    override fun getSchemaType(): SchemaType {
        return SchemaType.embeddedSchema
    }

    override fun getRemoteSource(): String {
        return remoteSchemaUrl
    }
}