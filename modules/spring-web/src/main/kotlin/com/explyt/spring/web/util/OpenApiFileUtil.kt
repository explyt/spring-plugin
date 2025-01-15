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

package com.explyt.spring.web.util

import com.explyt.spring.web.builder.openapi.OpenApiBuilderFactory
import com.explyt.spring.web.editor.openapi.OpenApiUIEditor
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention
import com.explyt.spring.web.model.OpenApiSpecificationType
import com.explyt.spring.web.service.OpenApiLocalSpecifications
import com.explyt.spring.web.tracker.OpenApiLanguagesModificationTracker
import com.explyt.util.CacheKeyStore
import com.intellij.ide.scratch.RootType
import com.intellij.json.JsonFileType
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.yaml.YAMLFileType
import java.io.File

class OpenApiFileUtil {

    fun findSpecificationType(
        project: Project,
        psiFile: PsiFile,
        virtualFile: VirtualFile
    ): OpenApiSpecificationType {
        val key: Key<CachedValue<OpenApiSpecificationType>> = SPECIFICATION_TYPE_KEY
        return CachedValuesManager.getManager(project).getCachedValue(
            psiFile,
            key,
            {
                val result = getSpecificationType(virtualFile, psiFile)
                val modificationTracker = project.getService(OpenApiLanguagesModificationTracker::class.java)
                    ?: ModificationTracker.NEVER_CHANGED

                CachedValueProvider.Result.create(
                    result,
                    modificationTracker,
                    CacheKeyStore.cacheReset
                )
            },
            false
        )
    }

    fun findSpecificationTypeMore(file: VirtualFile, psiFile: PsiFile): OpenApiSpecificationType {
        val specificationType = psiFile.getUserData(SPECIFICATION_ICON_TYPE_KEY)
        specificationType?.let { return it }

        if (!isValidFileType(file)) return OpenApiSpecificationType.OpenApiUndefined

        val project = psiFile.project
        val fileUrl = file.url
        val localSpecificationType = project.getService(OpenApiLocalSpecifications::class.java)
            ?.getSpecificationType(fileUrl) ?: OpenApiSpecificationType.OpenApiUndefined

        return localSpecificationType.takeUnless { it is OpenApiSpecificationType.OpenApiUndefined }
            ?: findSpecificationType(psiFile.project, psiFile, file)
    }

    private fun isValidFileType(file: VirtualFile): Boolean {
        return when {
            file.fileType is JsonFileType -> true
            file.fileType is YAMLFileType -> true
            checkFile(file) -> {
                val extension = file.extension
                extension == "yaml" || extension == "yml" || extension == "json"
            }

            else -> false
        }
    }

    private fun isOpenApiSpecificationFile(psiFile: PsiFile, file: VirtualFile): Boolean {
        if (file.nameWithoutExtension == SpringWebUtil.OPEN_API) {
            return true
        }
        return psiFile.text?.contains(SpringWebUtil.OPEN_API) == true
    }

    fun isOpenApiFile(file: VirtualFile, psiFile: PsiFile?): Boolean {
        return !file.isDirectory
                && file.isValid
                && isValidFileType(file)
                && (psiFile == null || isOpenApiSpecificationFile(psiFile, file))
                && !SingleRootFileViewProvider.isTooLargeForIntelligence(file)
    }

    fun createAndShow(
        project: Project,
        endpointInfos: List<AddEndpointToOpenApiIntention.EndpointInfo>,
        servers: List<String>,
        layout: TextEditorWithPreview.Layout
    ) {
        val fileType = YAMLFileType.YML

        val file = File.createTempFile("openapi-", ".${fileType.defaultExtension}")
        file.deleteOnExit()
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return

        val openapiBuilder = OpenApiBuilderFactory.getOpenApiFileBuilder(fileType)
        val serversToAdd = servers.ifEmpty { listOf("http://localhost:8080") }
        for (server in serversToAdd) {
            openapiBuilder.addServer(server)
        }
        for (info in endpointInfos) {
            openapiBuilder.addEndpoint(info)
        }

        runWriteAction {
            VfsUtil.saveText(virtualFile, openapiBuilder.toString())
            val openFileDescriptor = OpenFileDescriptor(project, virtualFile)
            val openapiEditor = FileEditorManager.getInstance(project)
                .openEditor(openFileDescriptor, true)
                .firstNotNullOfOrNull { it as? OpenApiUIEditor }
                ?: return@runWriteAction
            openapiEditor.showPreview("", layout)
        }
    }

    private fun getSpecificationType(file: VirtualFile, psiFile: PsiFile): OpenApiSpecificationType {
        return when {
            !isOpenApiFile(file, psiFile) -> OpenApiSpecificationType.OpenApiUndefined
            isOpenApiV30File(file, psiFile) -> OpenApiSpecificationType.OpenApiV30.INSTANCE
            isOpenApiV31File(file, psiFile) -> OpenApiSpecificationType.OpenApiV31.INSTANCE
            else -> OpenApiSpecificationType.OpenApiUndefined
        }
    }

    private fun isOpenApiV30File(file: VirtualFile, psiFile: PsiFile): Boolean {
        return isSpecificationFileType(file, psiFile, reg30)
    }

    private fun isOpenApiV31File(file: VirtualFile, psiFile: PsiFile): Boolean {
        return isSpecificationFileType(file, psiFile, reg31)
    }

    private fun isSpecificationFileType(
        file: VirtualFile,
        psiFile: PsiFile,
        versionRegex: Regex
    ): Boolean {
        val fileText = psiFile.text ?: LoadTextUtil.loadText(file)
        return versionRegex.containsMatchIn(fileText)
    }

    private fun checkFile(file: VirtualFile): Boolean {
        val rootType = RootType.forFile(file)
        return rootType != null && !rootType.isHidden
    }

    companion object {
        val INSTANCE: OpenApiFileUtil = OpenApiFileUtil()
        val SPECIFICATION_TYPE_KEY: Key<CachedValue<OpenApiSpecificationType>> = Key.create("openApiSpecificationType")
        val SPECIFICATION_ICON_TYPE_KEY: Key<OpenApiSpecificationType> = Key.create("openApiIconSpecificationType")

        private var reg30: Regex
        private var reg31: Regex

        init {
            reg30 = createRegex(SpringWebUtil.OPEN_API, "3\\.0\\.\\d+(-.+)?")
            reg31 = createRegex(SpringWebUtil.OPEN_API, "3\\.1\\.\\d+(-.+)?")
        }

        private fun createRegex(placeholder: String, versionPattern: String): Regex {
            val pattern = "[\"']?$placeholder[\"']?\\s*:\\s*[\"']?$versionPattern[\"']?"
            return Regex(pattern)
        }
    }

}
