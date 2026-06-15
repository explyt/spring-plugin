/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.inspection

import com.explyt.base.LibraryClassCache
import com.explyt.util.SpringBaseClasses.CORE_ENVIRONMENT
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.psi.PsiFile

abstract class SpringBaseLocalInspectionTool : LocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return LibraryClassCache.searchForLibraryClass(file.project, CORE_ENVIRONMENT) != null
    }
}