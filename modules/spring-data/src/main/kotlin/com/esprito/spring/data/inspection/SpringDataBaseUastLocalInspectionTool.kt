package com.esprito.spring.data.inspection

import com.esprito.base.LibraryClassCache
import com.esprito.inspection.SpringBaseUastLocalInspectionTool
import com.esprito.spring.data.SpringDataClasses
import com.intellij.psi.PsiFile

abstract class SpringDataBaseUastLocalInspectionTool : SpringBaseUastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return LibraryClassCache.searchForLibraryClass(file.project, SpringDataClasses.SPRING_RESOURCE) != null
    }
}