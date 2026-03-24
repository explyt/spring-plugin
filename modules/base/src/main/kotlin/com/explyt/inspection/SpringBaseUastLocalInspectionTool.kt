/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.inspection

import com.explyt.base.LibraryClassCache
import com.explyt.plugin.PluginIds
import com.explyt.util.SpringBaseClasses.CORE_ENVIRONMENT
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.psi.PsiFile

abstract class SpringBaseUastLocalInspectionTool : AbstractBaseUastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        //spring boot is ultimate plugin. it is mean that inspection not available in Paid version,
        // because it already has spring bundle plugin
        return PluginIds.SPRING_BOOT_JB.isNotEnabled()
                && LibraryClassCache.searchForLibraryClass(file.project, CORE_ENVIRONMENT) != null
    }
}