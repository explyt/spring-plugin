/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data.inspection

import com.explyt.base.LibraryClassCache
import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.plugin.PluginIds
import com.explyt.spring.data.SpringDataClasses
import com.intellij.psi.PsiFile

abstract class SpringDataBaseUastLocalInspectionTool : SpringBaseUastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return PluginIds.SPRING_DATA_JB.isNotEnabled() && LibraryClassCache.searchForLibraryClass(
            file.project,
            SpringDataClasses.REPOSITORY
        ) != null
    }
}