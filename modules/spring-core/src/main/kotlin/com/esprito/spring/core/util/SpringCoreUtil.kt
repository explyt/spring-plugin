package com.esprito.spring.core.util

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.properties.SpringPropertySourceSearch
import com.esprito.util.ModuleUtil
import com.esprito.util.runReadNonBlocking
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile

object SpringCoreUtil {

    fun isConfigurationPropertyFile(psiFile: PsiFile): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(psiFile) ?: return false
        if (!isSpringProject(module)) {
            return false
        }

        val fileExtension = FileUtilRt.getExtension(psiFile.name)
        if (!arrayOf("properties", "yaml", "yml").contains(fileExtension)) {
            return false
        }

        val fileName = FileUtilRt.getNameWithoutExtension(psiFile.name)
        if (fileName == "application"
            || fileName.startsWith("application-")
            || "config" == psiFile.parent?.name
        ) {
            return true
        }

        return runReadNonBlocking {
            val propertySourceFilePaths = SpringPropertySourceSearch.getInstance(psiFile.project)
                .findPropertySourceFilePaths(psiFile)
            if (propertySourceFilePaths.isEmpty()) {
                return@runReadNonBlocking false
            }

            val propertiesVf = psiFile.virtualFile ?: return@runReadNonBlocking false
            val sourceRootVf = ModuleUtil.getSourceRootFile(psiFile) ?: return@runReadNonBlocking false

            return@runReadNonBlocking propertySourceFilePaths.any { propertyFilePath ->
                val purePath = propertyFilePath.substringAfter("classpath*:")
                    .substringAfter("classpath:")
                    .substringAfter("file:")

                if (purePath.substringAfterLast("/") != psiFile.name) {
                    return@any false
                }

                val targetFile = VfsUtil.findRelativeFile(purePath, sourceRootVf)
                return@any targetFile == propertiesVf
            }
        }
    }

    fun isSpringProject(module: Module): Boolean {
        return LibraryClassCache.searchForLibraryClass(module, SpringCoreClasses.COMPONENT) != null
    }

    fun isSpringBootProject(project: Project): Boolean {
        return LibraryClassCache.searchForLibraryClass(
            project,
            SpringCoreClasses.SPRING_BOOT_APPLICATION
        ) != null
    }
}
