package com.explyt.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.JavaProjectRootsUtil
import com.intellij.openapi.roots.ModuleRootManagerEx
import com.intellij.openapi.vfs.VirtualFile

object SourcesUtils {
    fun getSourceRoots(module: Module, isTests: Boolean = false): List<VirtualFile> {
        return collectRootsByTypeSuffix(module, "SourceRootType", isTests)
    }

    fun getResourceRoots(module: Module, isTests: Boolean = false): List<VirtualFile> {
        return collectRootsByTypeSuffix(module, "ResourceRootType", isTests)
    }

    private fun collectRootsByTypeSuffix(module: Module, suffix: String, isTests: Boolean): List<VirtualFile> {
        if (module.isDisposed) return emptyList()
        return ModuleRootManagerEx.getInstance(module).contentEntries.flatMap {
            it.sourceFolders.asSequence()
        }.filter {
            !JavaProjectRootsUtil.isForGeneratedSources(it)
                    && it.isTestSource == isTests
                    && it.rootType::class.java.name.endsWith(suffix)
        }.mapNotNull { it.file }
    }
}