package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.util.SpringCoreUtil.isAdditionalConfigFile
import com.intellij.json.JsonLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

class SpringMetadataJsonSchemaFileProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project): MutableList<JsonSchemaFileProvider> {
        return mutableListOf(
            SpringMetadataJsonSchemaFileProvider(
                SpringCoreBundle.message("explyt.spring.metadata.config.schema.name"),
                project
            )
        )
    }
}

class SpringMetadataJsonSchemaFileProvider(
    private val visibleName: String,
    private val project: Project
) : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean {
        if (!file.isValid) return false
        return ApplicationManager.getApplication().runReadAction(Computable {
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@Computable false
            isAdditionalConfigFile(psiFile)
        })
    }

    override fun getName(): String {
        return visibleName
    }

    override fun getSchemaFile(): VirtualFile {
        val fileName = "spring-metadata.json"
        val inputStream = this.javaClass.classLoader.getResourceAsStream("schema/$fileName")
            ?: throw RuntimeException("No bundled json schema found in resources folder: 'schema/$fileName'")

        val defaultSchemaContent = FileUtil.loadTextAndClose(inputStream)

        val jsonFileType = FileTypeManager.getInstance().findFileTypeByLanguage(JsonLanguage.INSTANCE)
        return LightVirtualFile(fileName, jsonFileType, defaultSchemaContent)
    }

    override fun getSchemaType(): SchemaType {
        return SchemaType.embeddedSchema
    }

}
