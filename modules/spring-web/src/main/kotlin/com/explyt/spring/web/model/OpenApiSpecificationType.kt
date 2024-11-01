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

package com.explyt.spring.web.model

import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.util.OpenApiFileHelper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

open class OpenApiSpecificationType(val presentableName: String) {

    open class OpenAPI3Components(presentableName: String) :
        OpenApiSpecificationType(presentableName)

    open class OpenAPI30Components :
        OpenAPI3Components(SpringWebBundle.message("explyt.openapi.3.0.schema.name")) {
        override fun toString(): String {
            return SpringWebBundle.message("explyt.openapi.3.0.schema.type")
        }
    }

    open class OpenAPI31Components :
        OpenAPI3Components(SpringWebBundle.message("explyt.openapi.3.1.schema.name")) {
        override fun toString(): String {
            return SpringWebBundle.message("explyt.openapi.3.1.schema.type")
        }
    }

    object OpenApi30 : OpenAPI30Components() {
        val INSTANCE: OpenApi30 = OpenApi30
    }

    object OpenApi31 : OpenAPI31Components() {
        val INSTANCE: OpenApi31 = OpenApi31
    }

    interface SpecificationExtension {
        val partSchemaId: String
    }

    data class OpenAPI30SpecificationExtension(override val partSchemaId: String) : OpenAPI30Components(),
        SpecificationExtension

    data class Openapi31SpecificationExtension(override val partSchemaId: String) : OpenAPI30Components(),
        SpecificationExtension


    open class NONE(private val stringValue: String) :
        OpenApiSpecificationType(SpringWebBundle.message("explyt.openapi.unknown.specification")) {

        override fun toString(): String {
            return this.stringValue
        }
    }

    object UNKNOWN : NONE("unknown") {
        val INSTANCE: UNKNOWN = UNKNOWN
    }
}

object OpenApiSpecificationDetection {
    fun detectPrimarySpecificationType(virtualFile: VirtualFile, psiFile: PsiFile): OpenApiSpecificationType {
        return OpenApiFileHelper.INSTANCE.getOrComputePrimarySpecificationType(psiFile.project, psiFile, virtualFile)
    }

    fun detectSpecificationType(virtualFile: VirtualFile, psiFile: PsiFile): OpenApiSpecificationType {
        return OpenApiFileHelper.INSTANCE.getOrComputeSpecificationType(virtualFile, psiFile)
    }

}