/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import ai.grazie.utils.toLinkedSet
import com.explyt.spring.web.builder.openapi.OpenApiBuilderFactory
import com.explyt.spring.web.editor.openapi.OpenApiUIEditor
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention
import com.explyt.spring.web.model.OpenApiSpecificationType
import com.explyt.spring.web.providers.EndpointRunLineMarkerProvider
import com.explyt.spring.web.service.OpenApiLocalSpecifications
import com.explyt.spring.web.tracker.OpenApiLanguagesModificationTracker
import com.explyt.util.CacheKeyStore
import com.intellij.ide.scratch.RootType
import com.intellij.json.JsonFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
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
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.yaml.YAMLFileType
import java.io.File
import java.util.concurrent.Callable

class OpenApiFileUtil {

    fun findSpecificationType(
        psiFile: PsiFile
    ): OpenApiSpecificationType {
        return CachedValuesManager.getManager(psiFile.project).getCachedValue(
            psiFile,
            {
                val virtualFile = psiFile.virtualFile
                val result = virtualFile?.let { getSpecificationType(it, psiFile) }
                    ?: OpenApiSpecificationType.OpenApiUndefined
                val modificationTracker = psiFile.project.getService(OpenApiLanguagesModificationTracker::class.java)
                    ?: ModificationTracker.NEVER_CHANGED

                CachedValueProvider.Result.create(
                    result,
                    modificationTracker,
                    CacheKeyStore.cacheReset
                )
            }
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
            ?: findSpecificationType(psiFile)
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
        layout: TextEditorWithPreview.Layout,
        endpointServers: List<String>? = null
    ) {
        ReadAction.nonBlocking(Callable {
            buildContent(endpointInfos, servers, endpointServers ?: endpointServersOf(endpointInfos))
        })
            .inSmartMode(project)
            .expireWhen { project.isDisposed }
            .submit(AppExecutorUtil.getAppExecutorService())
            .onSuccess { content ->
                AppExecutorUtil.getAppExecutorService().execute {
                    val file = File.createTempFile("openapi-", ".${YAMLFileType.YML.defaultExtension}")
                    file.deleteOnExit()
                    val document = OpenApiDocument(file, content)
                    ApplicationManager.getApplication().invokeLater({
                        show(project, document, layout)
                    }, ModalityState.nonModal(), project.disposed)
                }
            }
    }

    private fun buildContent(
        endpointInfos: List<AddEndpointToOpenApiIntention.EndpointInfo>,
        servers: List<String>,
        endpointServers: List<String>
    ): String {
        val openapiBuilder = OpenApiBuilderFactory.getOpenApiFileBuilder(YAMLFileType.YML)
        val serversToAdd = servers.ifEmpty { listOf(DEFAULT_SERVER) }
        val allServers = LinkedHashSet<String>(8)
        allServers.addAll(endpointServers)
        allServers.addAll(serversToAdd)
        for (server in allServers) {
            openapiBuilder.addServer(server)
        }
        for (info in endpointInfos) {
            openapiBuilder.addEndpoint(info)
        }
        return openapiBuilder.toString()
    }

    private fun show(project: Project, document: OpenApiDocument, layout: TextEditorWithPreview.Layout) {
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(document.file) ?: return
        runWriteAction {
            VfsUtil.saveText(virtualFile, document.content)
            val openFileDescriptor = OpenFileDescriptor(project, virtualFile)
            val openapiEditor = FileEditorManager.getInstance(project)
                .openEditor(openFileDescriptor, true)
                .firstNotNullOfOrNull { it as? OpenApiUIEditor }
                ?: return@runWriteAction
            openapiEditor.showPreview("", layout)
        }
    }

    fun endpointServersOf(endpointInfos: List<AddEndpointToOpenApiIntention.EndpointInfo>): List<String> =
        endpointInfos.firstOrNull()
            ?.let { EndpointRunLineMarkerProvider.applyServerPortSettings(it.psiElement) }
            .orEmpty()

    private data class OpenApiDocument(val file: File, val content: String)

    private fun getSpecificationType(file: VirtualFile, psiFile: PsiFile): OpenApiSpecificationType {
        return when {
            !isOpenApiFile(file, psiFile) -> OpenApiSpecificationType.OpenApiUndefined
            isOpenApiV30File(file, psiFile) -> OpenApiSpecificationType.OpenApiV30.INSTANCE
            isOpenApiV31File(file, psiFile) -> OpenApiSpecificationType.OpenApiV31.INSTANCE
            isOpenApiV32File(file, psiFile) -> OpenApiSpecificationType.OpenApiV32.INSTANCE
            else -> OpenApiSpecificationType.OpenApiUndefined
        }
    }

    private fun isOpenApiV30File(file: VirtualFile, psiFile: PsiFile): Boolean {
        return isSpecificationFileType(file, psiFile, reg30)
    }

    private fun isOpenApiV31File(file: VirtualFile, psiFile: PsiFile): Boolean {
        return isSpecificationFileType(file, psiFile, reg31)
    }

    private fun isOpenApiV32File(file: VirtualFile, psiFile: PsiFile): Boolean {
        return isSpecificationFileType(file, psiFile, reg32)
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
        val SPECIFICATION_ICON_TYPE_KEY: Key<OpenApiSpecificationType> = Key.create("openApiIconSpecificationType")

        const val DEFAULT_SERVER = "http://localhost:8080"
        const val DEFAULT_SERVER_HOST = "http://localhost:"

        private var reg30: Regex
        private var reg31: Regex
        private var reg32: Regex

        init {
            reg30 = createRegex(SpringWebUtil.OPEN_API, "3\\.0\\.\\d+(-.+)?")
            reg31 = createRegex(SpringWebUtil.OPEN_API, "3\\.1\\.\\d+(-.+)?")
            reg32 = createRegex(SpringWebUtil.OPEN_API, "3\\.2\\.\\d+(-.+)?")
        }

        private fun createRegex(placeholder: String, versionPattern: String): Regex {
            val pattern = "[\"']?$placeholder[\"']?\\s*:\\s*[\"']?$versionPattern[\"']?"
            return Regex(pattern)
        }
    }

}
