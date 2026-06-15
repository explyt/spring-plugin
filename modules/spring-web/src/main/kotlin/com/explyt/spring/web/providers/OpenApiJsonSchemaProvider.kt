/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.web.model.OpenApiSpecificationFinder
import com.explyt.spring.web.model.OpenApiSpecificationType
import com.explyt.spring.web.service.OpenApiSpecificationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.SchemaType

class OpenApiJsonSchemaProvider(
    private val project: Project,
    private val specificationType: OpenApiSpecificationType,
    private val name: String,
    private val remoteSource: String
) : JsonSchemaFileProvider {

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema
    override fun getRemoteSource(): String = remoteSource

    override fun isAvailable(file: VirtualFile): Boolean {
        if (!file.isValid) return false
        val specificationTypeFound = findSpecificationTypeSafely(file)
        return specificationTypeFound == specificationType
    }

    private fun findSpecificationTypeSafely(file: VirtualFile): OpenApiSpecificationType {
        return runReadAction {
            val psiFile = PsiManager.getInstance(project).findFile(file)
                ?: return@runReadAction OpenApiSpecificationType.OpenApiUndefined
            OpenApiSpecificationFinder.findSpecificationType(file, psiFile)
        }
    }

    override fun getName(): String {
        return name
    }

    override fun getSchemaFile(): VirtualFile? =
        project.getService(OpenApiSpecificationManager::class.java)
            ?.getSchemaByFile(specificationType)
            ?.file

}