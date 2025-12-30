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

package com.explyt.spring.core.util

import com.explyt.spring.core.util.SpringCoreUtil.SPRING_BOOT_MAVEN
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.concurrency.annotations.RequiresReadLock


private const val SPRING_BOOT_STARTER_SUFFIX = "-starter"
private const val SPRING_BOOT_MAIN_STARTER = ":spring-boot-starter:"

object SpringBootUtil {

    @RequiresReadLock
    fun getSpringBootVersion(psiElement: PsiElement): String {
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return ""
        return CachedValuesManager.getManager(psiElement.project).getCachedValue(module) {
            val libraryVersion = JavaLibraryUtil.getLibraryVersion(module, SPRING_BOOT_MAVEN) ?: ""
            CachedValueProvider.Result.create(
                libraryVersion,
                ProjectRootManager.getInstance(module.project)
            )
        }
    }

    @RequiresReadLock
    fun getSpringBootStarters(psiElement: PsiElement): List<String> {
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return emptyList()
        val entries = ModuleRootManager.getInstance(module).orderEntries()
        val libraries = mutableListOf<String>()
        entries.forEachLibrary {
            val name = it?.name ?: return@forEachLibrary true
            if (name.contains(SPRING_BOOT_MAIN_STARTER)) return@forEachLibrary true
            if (name.contains(SPRING_BOOT_STARTER_SUFFIX)) {
                val starterArtifactId = name.split(":").reversed()
                    .firstOrNull { part -> part.contains(SPRING_BOOT_STARTER_SUFFIX) }
                starterArtifactId?.let { libraries.add(it) }
            }
            true
        }
        return libraries
    }

}