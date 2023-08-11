package com.esprito.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiFileReferenceHelper

object ModuleUtil {

    fun getSourceRootFile(psiElement: PsiElement): VirtualFile? {
        val vf = psiElement.containingFile.virtualFile ?: return null
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return null

        return ModuleRootManager.getInstance(module)
            .contentEntries.asSequence()
            .flatMap { contentEntry ->
                contentEntry.sourceFolders.asSequence().mapNotNull { it.file }
            }.filter {
                VfsUtil.isAncestor(it, vf, false)
            }.firstOrNull()
    }

    fun getContentRootFile(psiElement: PsiElement): VirtualFile? {
        return psiElement.project.guessProjectDir()
        // this work too
        // return  PsiFileReferenceHelper.getInstance().findRoot(psiElement.project, psiElement.containingFile.virtualFile)

    }
}