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
import com.intellij.openapi.roots.JavaProjectRootsUtil
import com.intellij.openapi.roots.ModuleRootManagerEx
import com.intellij.openapi.vfs.VirtualFile

object SourcesUtils {
    fun getSourceRoots(module: Module, isTests: Boolean = false): List<VirtualFile> {
        return collectRootsByTypeSuffix(module, "SourceRootType", isTests)
    }

    fun getResourceRoots(module: Module, isTests: Boolean = false): List<VirtualFile> {
        return if (module.name == "light_idea_test_case")
            collectRootsByTypeSuffix(module, "SourceRootType", false)
        else collectRootsByTypeSuffix(module, "ResourceRootType", isTests)
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