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