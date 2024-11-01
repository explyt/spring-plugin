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

package com.explyt.module

import com.explyt.util.ModuleUtil
import com.explyt.util.SourcesUtils
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

interface ExternalSystemModule {
    val ideaModules: List<Module>
        get() = listOfNotNull(rootModule, mainModule, testModule)

    val project: Project
        get() = ideaModules.first().project

    val rootModule: Module

    val mainModule: Module?
        get() = rootModule

    val testModule: Module?
        get() = rootModule

    val buildMetaInfDirectory: File?

    val sourceMetaInfDirectory: PsiDirectory?
        get() {
            val rootProjectPath = resourceRoots.firstOrNull() ?: return null
            return rootProjectPath.subdirectories.asSequence().filter { it.name == "META-INF" }.firstOrNull() ?: return null
        }

    val sourceRoots: List<PsiDirectory>

    val resourceRoots: List<PsiDirectory>

    val testSourceRoots: List<PsiDirectory>

    val testResourceRoots: List<PsiDirectory>

    val librariesSearchScope: GlobalSearchScope

    companion object {
        fun of(ideaModule: Module): ExternalSystemModule = ExternalSystemModuleManager.getInstance(ideaModule.project).getExternalSystemModuleModule(ideaModule)
    }
}

class GradleModule(override val rootModule: Module, override val mainModule: Module?, override val testModule: Module?) :
    ExternalSystemModule {

    override val buildMetaInfDirectory: File?
        get() {
            val rootProjectPath =
                ExternalSystemModulePropertyManager.getInstance(rootModule).getRootProjectPath() ?: return null
            val classesDir = Paths.get(rootProjectPath, "build", "classes")
            if (!classesDir.exists()) {
                return null
            }
            Files.walk(classesDir, 3).use { files ->
                return files.filter {
                    it.isDirectory() && it.name == "META-INF"
                }.findFirst().orElse(null)?.toFile()
            }
        }

    override val sourceRoots: List<PsiDirectory>
        get() {
            val mainIdeaModule = mainModule ?: return emptyList()
            return SourcesUtils.getSourceRoots(mainIdeaModule, isTests = false).toPsiDirectories(mainIdeaModule.project)
        }

    override val resourceRoots: List<PsiDirectory>
        get() {
            val mainIdeaModule = mainModule ?: return emptyList()
            return SourcesUtils.getResourceRoots(mainIdeaModule, isTests = false)
                .toPsiDirectories(mainIdeaModule.project)
        }

    override val testSourceRoots: List<PsiDirectory>
        get() {
            val testIdeaModule = testModule ?: return emptyList()
            return SourcesUtils.getSourceRoots(testIdeaModule, isTests = true).toPsiDirectories(testIdeaModule.project)
        }

    override val testResourceRoots: List<PsiDirectory>
        get() {
            val testIdeaModule = testModule ?: return emptyList()
            return SourcesUtils.getResourceRoots(testIdeaModule, isTests = true)
                .toPsiDirectories(testIdeaModule.project)
        }
    override val librariesSearchScope: GlobalSearchScope
        get() {
            val mainModule = mainModule ?: return GlobalSearchScope.EMPTY_SCOPE
            return ModuleUtil.getOnlyLibrarySearchScope(mainModule)
        }
}

class MavenModule(override val rootModule: Module) : ExternalSystemModule {

    override val buildMetaInfDirectory: File?
        get() {
            val rootProjectPath = ExternalSystemModulePropertyManager.getInstance(rootModule).getRootProjectPath()
                ?: rootModule.project.guessProjectDir()?.path
                ?: return null
            val metaInfFile = File(rootProjectPath, "target/classes/META-INF")
            if (metaInfFile.exists()) {
                return metaInfFile
            }
            return null
        }

    override val sourceRoots: List<PsiDirectory>
        get() = SourcesUtils.getSourceRoots(rootModule, isTests = false).toPsiDirectories(rootModule.project)

    override val resourceRoots: List<PsiDirectory>
        get() = SourcesUtils.getResourceRoots(rootModule, isTests = false).toPsiDirectories(rootModule.project)

    override val testSourceRoots: List<PsiDirectory>
        get() = SourcesUtils.getSourceRoots(rootModule, isTests = true).toPsiDirectories(rootModule.project)

    override val testResourceRoots: List<PsiDirectory>
        get() = SourcesUtils.getResourceRoots(rootModule, isTests = true).toPsiDirectories(rootModule.project)

    override val librariesSearchScope: GlobalSearchScope
        get() = ModuleUtil.getOnlyLibrarySearchScope(rootModule)
}

/**
 * ExternalSystemModule for projects, that do not linked to any external build system
 */
class ExternalSystemModuleImpl(override val rootModule: Module) : ExternalSystemModule {

    override val buildMetaInfDirectory: File?
        get() {
            return null
        }

    override val sourceRoots: List<PsiDirectory>
        get() = SourcesUtils.getSourceRoots(rootModule, isTests = false).toPsiDirectories(rootModule.project)

    override val resourceRoots: List<PsiDirectory>
        get() = SourcesUtils.getResourceRoots(rootModule, isTests = false).toPsiDirectories(rootModule.project)

    override val testSourceRoots: List<PsiDirectory>
        get() = SourcesUtils.getSourceRoots(rootModule, isTests = true).toPsiDirectories(rootModule.project)

    override val testResourceRoots: List<PsiDirectory>
        get() = SourcesUtils.getResourceRoots(rootModule, isTests = true).toPsiDirectories(rootModule.project)

    override val librariesSearchScope: GlobalSearchScope
        get() = ModuleUtil.getOnlyLibrarySearchScope(rootModule)
}

private fun List<VirtualFile>.toPsiDirectories(project: Project) = mapNotNull {
    PsiManager.getInstance(project).findDirectory(it)
}