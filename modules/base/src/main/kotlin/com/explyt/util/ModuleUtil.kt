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

package com.explyt.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopeUtil
import org.jetbrains.kotlin.idea.base.projectStructure.hasKotlinJvmRuntime
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.base.util.projectScope

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
    }

    fun getOnlyLibrarySearchScope(project: Project): GlobalSearchScope {
        return GlobalSearchScopeUtil.toGlobalSearchScope(
            project.allScope().intersectWith(GlobalSearchScope.notScope(project.projectScope())),
            project
        )
    }

    fun getOnlyLibrarySearchScope(module: Module): GlobalSearchScope {
        return GlobalSearchScopeUtil.toGlobalSearchScope(
            module.moduleWithLibrariesScope.intersectWith(GlobalSearchScope.notScope(module.moduleScope)),
            module.project
        )
    }

    fun isKotlinModule(module: Module): Boolean {
        val scope = module.getModuleWithDependenciesAndLibrariesScope(false)
        return scope.hasKotlinJvmRuntime(module.project)
    }
}