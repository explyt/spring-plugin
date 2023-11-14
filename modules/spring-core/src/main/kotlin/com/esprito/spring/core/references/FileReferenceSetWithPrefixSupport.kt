package com.esprito.spring.core.references

import com.esprito.util.ModuleUtil
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