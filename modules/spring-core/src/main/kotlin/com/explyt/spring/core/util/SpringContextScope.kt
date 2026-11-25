/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile

import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

class SpringContextScope private constructor(
    private val fileIndex: ProjectFileIndex,
    val modules: Set<Module>,
    val libraries: Set<Library>,
) {


    fun containsSource(file: VirtualFile): Boolean = fileIndex.getModuleForFile(file) in modules

    fun containsLibraryFile(file: VirtualFile): Boolean =
        fileIndex.getOrderEntriesForFile(file).any { (it as? LibraryOrderEntry)?.library in libraries }

    companion object {

        fun of(module: Module): SpringContextScope =
            CachedValuesManager.getManager(module.project).getCachedValue(module) {
                CachedValueProvider.Result(build(module), ProjectRootModificationTracker.getInstance(module.project))
            }

        private fun build(module: Module): SpringContextScope {
            val modules = mutableSetOf<Module>()
            val libraries = mutableSetOf<Library>()
            for (runner in ModuleUtilCore.getAllDependentModules(module) + module) {
                modules += runner
                val classpath = OrderEnumerator.orderEntries(runner).recursively()
                classpath.forEachModule { modules += it; true }
                classpath.forEachLibrary { libraries += it; true }
            }
            return SpringContextScope(ProjectFileIndex.getInstance(module.project), modules, libraries)
        }
    }
}
