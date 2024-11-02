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

package com.explyt.spring.core.references

import com.explyt.util.ModuleUtil
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet

enum class ReferenceType { FILE, ABSOLUTE_PATH, CLASSPATH }

class FileReferenceSetWithPrefixSupport(
    str: String,
    element: PsiElement,
    startInElement: Int,
    provider: PsiReferenceProvider?,
    suitableFileTypes: Array<FileType>?,
    private val type: ReferenceType?,
    private val needHardFileTypeFilter: Boolean
) : FileReferenceSet(
    str,
    element,
    startInElement,
    provider,
    false,
    false,
    suitableFileTypes,
    true
) {

    override fun computeDefaultContexts(): MutableCollection<PsiFileSystemItem> {
        if (type == ReferenceType.FILE || type == ReferenceType.ABSOLUTE_PATH) {
            val root = ModuleUtil.getContentRootFile(element) ?: return mutableListOf()
            val psiDirectory = PsiManager.getInstance(element.project).findDirectory(root) ?: return mutableListOf()
            return mutableListOf(psiDirectory)
        }
        return super.computeDefaultContexts()
    }

    override fun getReferenceCompletionFilter(): Condition<PsiFileSystemItem> {
        if (needHardFileTypeFilter) {
            return Condition { it.isDirectory || suitableFileTypes.contains(it.virtualFile.fileType) }
        }
        return super.getReferenceCompletionFilter()
    }

    override fun isAbsolutePathReference(): Boolean {
        return true
    }

    override fun isSoft(): Boolean {
        return false
    }

}