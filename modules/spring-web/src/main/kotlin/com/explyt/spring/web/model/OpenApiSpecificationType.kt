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
