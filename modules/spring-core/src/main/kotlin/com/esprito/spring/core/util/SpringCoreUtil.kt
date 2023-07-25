package com.esprito.spring.core.util

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.util.ModuleUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiFile

object SpringCoreUtil {

    fun isPropertyFile(psiFile: PsiFile): Boolean {
        val fileName = FileUtilRt.getNameWithoutExtension(psiFile.name)
        val fileExtension = FileUtilRt.getExtension(psiFile.name)
        return arrayOf("properties", "yaml", "yml").contains(fileExtension)
                && (fileName == "application" || fileName.startsWith("application-"))
    }

    fun isSpringProject(module: Module): Boolean {
        return ModuleUtil.isClassAvailableInLibraries(module, SpringCoreClasses.COMPONENT)
    }

    fun isSpringBootProject(project: Project): Boolean {
        return LibraryClassCache.searchForLibraryClass(
            project,
            SpringCoreClasses.SPRING_BOOT_APPLICATION
        ) != null
    }
}
