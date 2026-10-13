/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.inspection

import com.explyt.base.LibraryClassCache
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiFile

object InspectionAvailability {

    fun isIndexReady(file: PsiFile): Boolean = !DumbService.isDumb(file.project)

    fun hasLibraryClass(file: PsiFile, fqn: String): Boolean =
        isIndexReady(file) && LibraryClassCache.searchForLibraryClass(file.project, fqn) != null
}
