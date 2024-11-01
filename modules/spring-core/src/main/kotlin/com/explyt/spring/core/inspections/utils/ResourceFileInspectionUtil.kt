/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.inspections.utils

import com.explyt.module.ExternalSystemModule
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringCoreClasses.ANNOTATIONS_WITH_FILE_REFERENCES_TO_PROPERTIES
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.SpringProperties.PREFIX_CLASSPATH
import com.explyt.spring.core.SpringProperties.PREFIX_CLASSPATH_STAR
import com.explyt.spring.core.SpringProperties.PREFIX_FILE
import com.explyt.spring.core.SpringProperties.PREFIX_HTTP
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.util.ModuleUtil
import com.intellij.codeInsight.daemon.quickFix.CreateFilePathFix
import com.intellij.codeInsight.daemon.quickFix.NewFileLocation
import com.intellij.codeInsight.daemon.quickFix.TargetDirectory
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.*
import com.intellij.psi.impl.file.PsiDirectoryFactory
import java.nio.file.Path
import java.util.function.Supplier
import kotlin.io.path.isRegularFile

object ResourceFileInspectionUtil {
    fun psiAnnotationPropertySourceMembers(aClass: PsiClass): Set<PsiAnnotationMemberValue> {
        val module = ModuleUtilCore.findModuleForPsiElement(aClass) ?: return emptySet()
        return ANNOTATIONS_WITH_FILE_REFERENCES_TO_PROPERTIES
            .asSequence()
            .map { SpringSearchService.getInstance(module.project).getMetaAnnotations(module, it) }
            .flatMap { it.getAnnotationMemberValues(aClass, setOf("value")) }
            .toSet()
    }

    fun psiAnnotationContextConfigurationMembers(aClass: PsiClass): Set<PsiAnnotationMemberValue> {
        val module = ModuleUtilCore.findModuleForPsiElement(aClass) ?: return emptySet()
        val metaHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.CONTEXT_CONFIGURATION)
        return metaHolder.getAnnotationMemberValues(aClass, setOf("value", "locations"))
    }

    fun getPathProblems(
        languageFileType: LanguageFileType,
        text: String,
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): List<ProblemDescriptor> {
        return (getPathProblemsClasspath(languageFileType, text, element, manager, isOnTheFly)
                + getPathProblemsWithPrefixFile(languageFileType, text, element, manager, isOnTheFly))
    }

    fun getPathProblemsWithPrefixFile(
        languageFileType: LanguageFileType,
        text: String,
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): List<ProblemDescriptor> {
        if (!text.startsWith(PREFIX_FILE)) return emptyList()

        val textWithoutPrefix = text.substringAfter(PREFIX_FILE)
        val rootPaths = getSourceRootsPath(textWithoutPrefix, element)
        return getPsiFileProblemFix(rootPaths, textWithoutPrefix, languageFileType, element, manager, isOnTheFly)
    }

    fun getPathProblemsClasspath(
        languageFileType: LanguageFileType,
        text: String,
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): List<ProblemDescriptor> {
        if (!text.startsWith(PREFIX_CLASSPATH) && !text.startsWith(PREFIX_CLASSPATH_STAR)) return emptyList()
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
        val rootPaths = getRootPaths(module)
        val textWithoutPrefix = getTextValue(text)
        return getPsiFileProblemFix(rootPaths, textWithoutPrefix, languageFileType, element, manager, isOnTheFly)
    }

    private val VALID_PREFIXES = setOf("/", PREFIX_CLASSPATH, PREFIX_FILE, PREFIX_HTTP)

    fun getPathClassResourceProblems(
        languageFileType: LanguageFileType,
        text: String,
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): List<ProblemDescriptor> {
        if (VALID_PREFIXES.none { text.startsWith(it) }) {
            return listOf(
                manager.createProblemDescriptor(
                    element,
                    SpringCoreBundle.message("explyt.spring.inspection.resource.error.start.with.slash"),
                    isOnTheFly,
                    emptyArray(),
                    ProblemHighlightType.WARNING
                )
            )
        }
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
        val rootPaths = getRootPaths(module)
        val textWithoutPrefix = if (text.startsWith("/")) text.substringAfter("/") else text

        return getPsiFileProblemFix(rootPaths, textWithoutPrefix, languageFileType, element, manager, isOnTheFly)
    }

    private fun getTextValue(text: String): String {
        return if (text.startsWith(PREFIX_CLASSPATH)) text.substringAfter(PREFIX_CLASSPATH) else
            text.substringAfter(PREFIX_CLASSPATH_STAR)
    }

    private fun getSourceRootsPath(textWithoutPrefix: String, element: PsiElement): List<PsiDirectory> {
        if (textWithoutPrefix.startsWith("/") || textWithoutPrefix.startsWith("./")) {
            return emptyList()
        }
        val directoryFactory = PsiDirectoryFactory.getInstance(element.project)
        val contentRootFiles = mutableListOf<PsiDirectory>()
        ModuleUtil.getContentRootFile(element)?.let { contentRootFiles.add(directoryFactory.createDirectory(it)) }
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return contentRootFiles
        contentRootFiles += ExternalSystemModule.of(module).sourceRoots
        return contentRootFiles
    }

    private fun getPsiFileProblemFix(
        rootPaths: List<PsiDirectory>,
        textWithoutPrefix: String,
        languageFileType: LanguageFileType,
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): List<ProblemDescriptor> {
        val highlightType = if (languageFileType == XmlFileType.INSTANCE)
            ProblemHighlightType.GENERIC_ERROR else ProblemHighlightType.WARNING

        val pathToFile = getPathToFile(textWithoutPrefix)
        val fileName = pathToFile.lastOrNull()?.toString() ?: return emptyList()
        val fileDirs = pathToFile.parent?.toList()?.map { it.toString() }?.toTypedArray() ?: emptyArray()

        val fileExist = rootPaths.mapNotNull { it.virtualFile.toNioPathOrNull()?.resolve(pathToFile) }
            .firstOrNull { it.isRegularFile() }
        if (fileExist != null) return emptyList()

        val rootPath = getRootPath(rootPaths, fileDirs) ?: return emptyList()
        val directory = TargetDirectory(rootPath, fileDirs)
        val location = NewFileLocation(listOf(directory), fileName)
        val fix = CreateFilePathAvailable(element, location) { getText(languageFileType, manager.project) }
        return listOf(
            manager.createProblemDescriptor(
                element,
                SpringCoreBundle.message("explyt.spring.inspection.file.resolve.error.message", fileName),
                isOnTheFly,
                arrayOf(fix),
                highlightType
            )
        )
    }

    private fun getPathToFile(textWithoutPrefix: String): Path {
        val path = Path.of(textWithoutPrefix)
        if (!path.isAbsolute) return path
        return try {
            path.subpath(0, path.nameCount)
        } catch (e: Exception) {
            path
        }
    }

    private fun getText(languageFileType: LanguageFileType, project: Project): String {
        if (languageFileType != XmlFileType.INSTANCE) return ""
        return FileTemplateManager.getInstance(project).getDefaultTemplate(SpringProperties.SPRING_XML_TEMPLATE).text
    }

    private fun getRootPaths(module: Module): List<PsiDirectory> {
        val exModule = ExternalSystemModule.of(module)
        return exModule.resourceRoots + exModule.sourceRoots
    }

    private fun getRootPath(rootPaths: List<PsiDirectory>, fileDirs: Array<String>): PsiDirectory? {
        if (fileDirs.isEmpty()) return rootPaths.firstOrNull()
        val firstSubDir = fileDirs[0]
        return rootPaths
            .firstOrNull { it.virtualFile.toNioPathOrNull()?.resolve(firstSubDir)?.isRegularFile() == true }
            ?: rootPaths.firstOrNull()
    }
}

private class CreateFilePathAvailable(
    psiElement: PsiElement, newFileLocation: NewFileLocation, fileTextSupplier: Supplier<String>
) : CreateFilePathFix(psiElement, newFileLocation, fileTextSupplier) {

    override fun isAvailable(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) = true
}
