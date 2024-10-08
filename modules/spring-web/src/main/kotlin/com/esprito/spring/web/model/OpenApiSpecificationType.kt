package com.esprito.spring.web.model

import com.esprito.spring.web.SpringWebBundle
import com.esprito.spring.web.util.OpenApiFileHelper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

open class OpenApiSpecificationType(val presentableName: String) {

    open class OpenAPI3Family(presentableName: String) :
        OpenApiSpecificationType(presentableName)

    open class OpenAPI30Family :
        OpenAPI3Family(SpringWebBundle.message("esprito.openapi.3.0.schema.name")) {
        override fun toString(): String {
            return SpringWebBundle.message("esprito.openapi.3.0.schema.type")
        }
    }

    open class OpenAPI31Family :
        OpenAPI3Family(SpringWebBundle.message("esprito.openapi.3.1.schema.name")) {
        override fun toString(): String {
            return SpringWebBundle.message("esprito.openapi.3.1.schema.type")
        }
    }

    object OpenApi30 : OpenAPI30Family() {
        val INSTANCE: OpenApi30 = OpenApi30
    }

    object OpenApi31 : OpenAPI31Family() {
        val INSTANCE: OpenApi31 = OpenApi31
    }

    interface SecondarySpecificationPart {
        val partSchemaId: String
    }

    data class Openapi30SecondarySpecificationPart(override val partSchemaId: String) : OpenAPI30Family(),
        SecondarySpecificationPart

    data class Openapi31SecondarySpecificationPart(override val partSchemaId: String) : OpenAPI30Family(),
        SecondarySpecificationPart


    open class NONE(private val stringValue: String) :
        OpenApiSpecificationType(SpringWebBundle.message("esprito.openapi.unknown.specification")) {

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