package com.esprito.spring.web.references

import com.esprito.module.ExternalSystemModule
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