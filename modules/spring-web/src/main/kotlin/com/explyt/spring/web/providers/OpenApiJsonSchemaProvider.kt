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