package com.esprito.util

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

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
}