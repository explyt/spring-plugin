/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
private const val SPRING_BOOT_MAIN_STARTER = "spring-boot-starter"
private const val SPRING_BOOT_KAFKA = "spring-kafka"

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
    fun getSpringBootStartersInfo(psiElement: PsiElement): Pair<String?, List<String>>? {
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return null
        val entries = ModuleRootManager.getInstance(module).orderEntries()
        val libraries = mutableListOf<String>()
        var buildTool: String? = null
        entries.forEachLibrary {
            val name = it?.name ?: return@forEachLibrary true
            if (buildTool == null) {
                buildTool = getBuildToolName(name)
            }
            if (name.contains(":$SPRING_BOOT_MAIN_STARTER:")) return@forEachLibrary true
            if (name.contains(":$SPRING_BOOT_KAFKA:")) {
                libraries.add(SPRING_BOOT_KAFKA)
            } else if (name.contains(SPRING_BOOT_STARTER_SUFFIX)) {
                val starterArtifactId = name.split(":").reversed()
                    .firstOrNull { part -> part.contains(SPRING_BOOT_STARTER_SUFFIX) }
                starterArtifactId?.let { libraries.add(it) }
            }
            true
        }
        return buildTool to libraries
    }
}

private const val GRADLE = "Gradle"
private const val MAVEN = "Maven"

private fun getBuildToolName(libraryName: String): String? {
    val buildToolPrefix = libraryName.split(":").firstOrNull() ?: return null
    return if (buildToolPrefix.contains(GRADLE, true)) {
        GRADLE
    } else if (buildToolPrefix.contains(MAVEN, true)) {
        MAVEN
    } else null
}
