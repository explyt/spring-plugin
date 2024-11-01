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

import com.explyt.spring.web.model.OpenApiSpecificationType
import com.explyt.spring.web.service.OpenApiUserDefinedSpecifications
import com.explyt.spring.web.tracker.OpenApiLanguagesModificationTracker
import com.explyt.util.CacheKeyStore
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.json.JsonFileType
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.yaml.YAMLFileType

class OpenApiFileHelper {

    fun getOrComputePrimarySpecificationType(
        project: Project,
        psiFile: PsiFile,
        virtualFile: VirtualFile
    ): OpenApiSpecificationType {
        val key: Key<CachedValue<OpenApiSpecificationType>> = SPECIFICATION_TYPE_KEY
        return CachedValuesManager.getManager(project).getCachedValue(
            psiFile,
            key,
            {
                val result = computeSpecificationType(virtualFile, psiFile)
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

    fun getOrComputeSpecificationType(virtualFile: VirtualFile, psiFile: PsiFile): OpenApiSpecificationType {
        val givenSpecificationType = psiFile.getUserData(SPECIFICATION_ICON_TYPE_KEY)
        if (givenSpecificationType != null) {
            return givenSpecificationType
        }

        val project = psiFile.project
        if (!isSuitableFileType(virtualFile)) {
            return OpenApiSpecificationType.UNKNOWN
        }

        val fileUrl = virtualFile.url
        val userDefinedSpecificationType = getUserDefinedSpecificationType(project, fileUrl)

        return when {
            userDefinedSpecificationType !is OpenApiSpecificationType.UNKNOWN -> userDefinedSpecificationType
            else -> getOrComputePrimarySpecificationType(project, psiFile, virtualFile)
        }
    }

    private fun getUserDefinedSpecificationType(project: Project, fileUrl: String): OpenApiSpecificationType {
        val service = project.getService(OpenApiUserDefinedSpecifications::class.java)
        return service?.getSpecificationType(fileUrl) ?: OpenApiSpecificationType.UNKNOWN
    }

    private fun isSuitableFileType(virtualFile: VirtualFile): Boolean {
        return if (virtualFile.fileType is YAMLFileType) {
            true
        } else if (virtualFile.fileType is JsonFileType) {
            true
        } else if (ScratchUtil.isScratch(virtualFile)) {
            val extension = virtualFile.extension
            extension == "yaml" || extension == "yml" || extension == "json"
        } else false
    }

    private fun isSpecificationFastCheck(virtualFile: VirtualFile, psiFile: PsiFile): Boolean {
        if (virtualFile.nameWithoutExtension == SpringWebUtil.OPEN_API) {
            return true
        }

        val name = psiFile.text ?: return false

        return name.contains(SpringWebUtil.OPEN_API)
    }

    fun isSuitableFile(virtualFile: VirtualFile, psiFile: PsiFile?): Boolean {
        return !virtualFile.isDirectory
                && virtualFile.isValid
                && isSuitableFileType(virtualFile)
                && !SingleRootFileViewProvider.isTooLargeForIntelligence(virtualFile)
                && (psiFile == null || isSpecificationFastCheck(virtualFile, psiFile))
    }

    private fun computeSpecificationType(virtualFile: VirtualFile, psiFile: PsiFile): OpenApiSpecificationType {
        return if (!isSuitableFile(virtualFile, psiFile)) {
            OpenApiSpecificationType.UNKNOWN.INSTANCE
        } else if (isOpenapi3File(virtualFile, psiFile)) {
            OpenApiSpecificationType.OpenApi30.INSTANCE
        } else if (isOpenapi31File(virtualFile, psiFile)) {
            OpenApiSpecificationType.OpenApi31.INSTANCE
        } else OpenApiSpecificationType.UNKNOWN.INSTANCE
    }

    private fun isOpenapi3File(virtualFile: VirtualFile, psiFile: PsiFile): Boolean {
        return isSpecificationFileOfKind(OPENAPI_3_0_PATTERN, virtualFile, psiFile)
    }

    private fun isOpenapi31File(virtualFile: VirtualFile, psiFile: PsiFile): Boolean {
        return isSpecificationFileOfKind(OPENAPI_3_1_PATTERN, virtualFile, psiFile)
    }

    private fun isSpecificationFileOfKind(
        specificationVersionPattern: Regex,
        virtualFile: VirtualFile,
        psiFile: PsiFile
    ): Boolean {
        if (psiFile.text != null) {
            return specificationVersionPattern.containsMatchIn(psiFile.text)
        }

        val textFile = LoadTextUtil.loadText(virtualFile)
        return specificationVersionPattern.containsMatchIn(textFile)
    }

    companion object {
        val INSTANCE: OpenApiFileHelper = OpenApiFileHelper()
        val SPECIFICATION_TYPE_KEY: Key<CachedValue<OpenApiSpecificationType>> = Key.create("openApiSpecificationType")

        val SPECIFICATION_ICON_TYPE_KEY: Key<OpenApiSpecificationType> = Key.create("openApiIconSpecificationType")

        private var OPENAPI_3_0_PATTERN: Regex
        private var OPENAPI_3_1_PATTERN: Regex

        init {
            val openApiVersionPattern30 = "[\"']?%s[\"']?\\s*:\\s*[\"']?%s[\"']?"
            val placeholders30 = arrayOf(SpringWebUtil.OPEN_API, "3\\.0\\.\\d+(-.+)?")
            val formattedPattern30 = String.format(openApiVersionPattern30, *placeholders30)
            OPENAPI_3_0_PATTERN = Regex(formattedPattern30)

            val openApiVersionPattern31 = "[\"']?%s[\"']?\\s*:\\s*[\"']?%s[\"']?"
            val placeholders31 = arrayOf(SpringWebUtil.OPEN_API, "3\\.1\\.\\d+(-.+)?")
            val formattedPattern31 = String.format(openApiVersionPattern31, *placeholders31)
            OPENAPI_3_1_PATTERN = Regex(formattedPattern31)
        }
    }

}
