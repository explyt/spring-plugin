package com.esprito.inspection

import com.esprito.base.LibraryClassCache
import com.esprito.util.SpringBaseClasses.CORE_ENVIRONMENT
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.psi.PsiFile

abstract class SpringBaseLocalInspectionTool() : LocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return LibraryClassCache.searchForLibraryClass(file.project, CORE_ENVIRONMENT) != null
    }
}