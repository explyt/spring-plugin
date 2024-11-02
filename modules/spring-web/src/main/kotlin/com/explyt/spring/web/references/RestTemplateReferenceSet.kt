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

package com.explyt.spring.web.references

import com.explyt.module.ExternalSystemModule
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet

class RestTemplateReferenceSet(
    path: String,
    element: PsiElement,
    startInElement: Int,
    private val prefix: String,
    suitableFileTypes: Array<FileType>?
) : FileReferenceSet(
    path,
    element,
    startInElement,
    null,
    false,
    false,
    suitableFileTypes,
    true
) {

    override fun computeDefaultContexts(): MutableCollection<PsiFileSystemItem> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return mutableListOf()

        val externalSystemModule = ExternalSystemModule.of(module)
        val context = if (prefix.startsWith("classpath:")) {
            externalSystemModule.resourceRoots
                .mapNotNull { findDirectory(it, prefix.removePrefix("classpath:")) }
        } else {
            externalSystemModule.sourceRoots
                .mapNotNull { it.parentDirectory }
                .mapNotNull { findDirectory(it, "webapp/$prefix") }
        }
        return mutableListOf(*context.toTypedArray())
    }

    private fun findDirectory(from: PsiDirectory, path: String): PsiDirectory? {
        val subdirectories = path.split("/")
            .filter { it.isNotBlank() }

        var currentDir: PsiDirectory? = from
        for (subDirectory in subdirectories) {
            currentDir = currentDir?.findSubdirectory(subDirectory) ?: return null
        }
        return currentDir
    }

    override fun getReferenceCompletionFilter(): Condition<PsiFileSystemItem> {
        return Condition { it.isDirectory || suitableFileTypes.contains(it.virtualFile.fileType) }
    }

    override fun isAbsolutePathReference(): Boolean {
        return true
    }

    override fun isSoft(): Boolean {
        return false
    }

}