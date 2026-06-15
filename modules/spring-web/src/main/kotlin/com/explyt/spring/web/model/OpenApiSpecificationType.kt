/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.model

import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.util.OpenApiFileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

open class OpenApiSpecificationType(val name: String) {

    open class OpenAPI3Components(viewName: String) : OpenApiSpecificationType(viewName)

    open class OpenApiV30 : OpenAPI3Components(SpringWebBundle.message("explyt.openapi.3.0.schema.name")) {
        override fun toString(): String {
            return SpringWebBundle.message("explyt.openapi.3.0.schema.type")
        }

        companion object {
            val INSTANCE = OpenApiV30()
        }
    }

    open class OpenApiV31 : OpenAPI3Components(SpringWebBundle.message("explyt.openapi.3.1.schema.name")) {
        override fun toString(): String {
            return SpringWebBundle.message("explyt.openapi.3.1.schema.type")
        }

        companion object {
            val INSTANCE = OpenApiV31()
        }
    }

    interface SpecificationExtension {
        val schemaExt: String
    }

    data class OpenAPI30SpecificationExtension(override val schemaExt: String) : OpenApiV30(),
        SpecificationExtension

    data class Openapi31SpecificationExtension(override val schemaExt: String) : OpenApiV31(),
        SpecificationExtension

    object OpenApiUndefined :
        OpenApiSpecificationType(SpringWebBundle.message("explyt.openapi.specification.undefined")) {
        override fun toString(): String {
            return "undefined"
        }
    }
}

object OpenApiSpecificationFinder {
    fun findSpecificationType(file: VirtualFile, psiFile: PsiFile): OpenApiSpecificationType {
        return OpenApiFileUtil.INSTANCE.findSpecificationType(psiFile.project, psiFile, file)
    }

    fun identifySpecificationType(file: VirtualFile, psiFile: PsiFile): OpenApiSpecificationType {
        return OpenApiFileUtil.INSTANCE.findSpecificationTypeMore(file, psiFile)
    }

}
