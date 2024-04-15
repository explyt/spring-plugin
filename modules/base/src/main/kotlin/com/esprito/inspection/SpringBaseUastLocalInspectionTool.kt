package com.esprito.inspection

import com.esprito.base.LibraryClassCache
import com.esprito.util.SpringBaseClasses.CORE_ENVIRONMENT
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.psi.PsiFile

abstract class SpringBaseUastLocalInspectionTool() : AbstractBaseUastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return LibraryClassCache.searchForLibraryClass(file.project, CORE_ENVIRONMENT) != null
    }
}