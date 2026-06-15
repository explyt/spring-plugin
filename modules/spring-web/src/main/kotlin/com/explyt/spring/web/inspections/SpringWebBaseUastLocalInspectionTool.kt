/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections

import com.explyt.base.LibraryClassCache
import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.plugin.PluginIds
import com.explyt.spring.web.SpringWebClasses
import com.intellij.psi.PsiFile

abstract class SpringWebBaseUastLocalInspectionTool : SpringBaseUastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return PluginIds.SPRING_WEB_JB.isNotEnabled() && LibraryClassCache.searchForLibraryClass(
            file.project,
            SpringWebClasses.WEB_INITIALIZER
        ) != null
    }
}