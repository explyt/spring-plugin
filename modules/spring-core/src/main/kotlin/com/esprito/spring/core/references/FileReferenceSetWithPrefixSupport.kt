package com.esprito.spring.core.references

import com.esprito.util.ModuleUtil
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Condition
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet

enum class REFERENCE_TYPE { FILE, ABSOLUTE_PATH, CLASSPATH }

class FileReferenceSetWithPrefixSupport(
    str: String,
    element: PsiElement,
    startInElement: Int,
    provider: PsiReferenceProvider?,
    isCaseSensitive: Boolean,
    endingSlashNotAllowed: Boolean,
    suitableFileTypes: Array<FileType>?,
    init: Boolean,
    private val type: REFERENCE_TYPE,
    private val needHardFileTypeFilter: Boolean
) : FileReferenceSet(
    str,
    element,
    startInElement,
    provider,
    isCaseSensitive,
    endingSlashNotAllowed,
    suitableFileTypes,
    init
) {

    override fun computeDefaultContexts(): MutableCollection<PsiFileSystemItem> {
        if (type == REFERENCE_TYPE.FILE || type == REFERENCE_TYPE.ABSOLUTE_PATH) {
            val root = ModuleUtil.getContentRootFile(element) ?: return mutableListOf()
            val psiDirectory = PsiManager.getInstance(element.project).findDirectory(root) ?: return mutableListOf()
            return mutableListOf(psiDirectory)
        }
        return super.computeDefaultContexts();
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