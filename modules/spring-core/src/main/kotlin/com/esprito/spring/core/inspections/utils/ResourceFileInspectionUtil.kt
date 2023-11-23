package com.esprito.spring.core.inspections.utils

import com.esprito.module.ExternalSystemModule
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringCoreClasses.ANNOTATIONS_WITH_FILE_REFERENCES_TO_PROPERTIES
import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.inspections.quickfix.ResourceFileQuickFix
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.util.ModuleUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

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

    fun getPathProblemsWithPrefixFile(
        typeFile: String,
        text: String,
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): List<ProblemDescriptor> {
        if (text.startsWith(SpringProperties.PREFIX_FILE)) {
            val textWithoutPrefix = text.substringAfter(SpringProperties.PREFIX_FILE)

            val paths = getSourceRootsPath(textWithoutPrefix, element)
            return getProblemsPaths(paths, typeFile, textWithoutPrefix, element, text, manager, isOnTheFly)
        }
        return emptyList()
    }

    private fun getSourceRootsPath(textWithoutPrefix: String, element: PsiElement): List<String> {
        if (textWithoutPrefix.startsWith("/") || textWithoutPrefix.startsWith("./")) {
            return listOf("")
        }
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return listOf("")
        val contentRootFiles = mutableListOf(ModuleUtil.getContentRootFile(element)?.path)
        contentRootFiles += ExternalSystemModule.of(module).sourceRoots.map { it.virtualFile.path }
        return contentRootFiles.filterNotNull()
    }

    fun getPathProblemsClasspath(
        typeFile: String,
        text: String,
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): List<ProblemDescriptor> {
        if (text.startsWith(SpringProperties.PREFIX_FILE)) {
            return emptyList()
        }
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
        val textWithoutPrefix = text.substringAfter(SpringProperties.PREFIX_CLASSPATH)

        val paths = mutableListOf<String>()
        paths += ExternalSystemModule.of(module).resourceRoots.map { it.virtualFile.path }
        paths += ExternalSystemModule.of(module).sourceRoots.map { it.virtualFile.path }

        return getProblemsPaths(paths, typeFile, textWithoutPrefix, element, text, manager, isOnTheFly)
    }

    private fun getProblemsPaths(
        paths: List<String>,
        typeFile: String,
        textWithoutPrefix: String,
        element: PsiElement,
        text: String,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): List<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()

        val pathList = Path.of(textWithoutPrefix).toList()
        val fileName = pathList.last()
        val dirs = if (pathList.size > 1) {
            pathList.slice(IntRange(0, pathList.size - 2))
                .filter { it.toString() != "." || !it.toString().startsWith("$") }
        } else {
            emptyList()
        }

        problems += getDirProblems(paths, typeFile, text, dirs, element, manager, isOnTheFly)
        problems += getFileProblem(paths, typeFile, text, fileName, element, manager, isOnTheFly)

        return problems
    }

    private fun getFileProblem(
        paths: List<String>,
        typeFile: String,
        text: String,
        fileName: Path,
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): List<ProblemDescriptor> {
        val range = ElementManipulators.getValueTextRange(element)
        val startOffset = text.indexOf(fileName.toString())
        val highlightType = if (typeFile == "XML") ProblemHighlightType.GENERIC_ERROR else ProblemHighlightType.WARNING

        val files = mutableMapOf<String, File>()
        val subPath = getSubPath(text, fileName.toString())
        for (path in paths) {
            val localPath = Paths.get(Path.of(File.separator + path).toString(), subPath) ?: continue
            files[localPath.toString()] = Paths.get(localPath.toString(), fileName.toString()).toFile()
        }

        val isFindFile = files.filter { it.value.isFile }.isNotEmpty()

        if (!isFindFile && startOffset != -1) {
            val curTextRange =
                TextRange(startOffset, startOffset + fileName.toString().length).shiftRight(range.startOffset)
            val path = files.asSequence()
                .filter { !it.value.isFile }
                .firstOrNull()?.key ?: return emptyList()
            return listOf(
                manager.createProblemDescriptor(
                    element,
                    curTextRange,
                    SpringCoreBundle.message("esprito.spring.inspection.file.resolve.error.message", fileName),
                    highlightType,
                    isOnTheFly,
                    ResourceFileQuickFix(path, fileName.toString(), false, typeFile)
                )
            )
        }
        return emptyList()
    }

    private fun getDirProblems(
        paths: List<String>,
        typeFile: String,
        text: String,
        dirs: List<Path>,
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): List<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val range = ElementManipulators.getValueTextRange(element)

        var pathDirs = ""
        var pathForRange = ""
        val highlightType = if (typeFile == "XML") ProblemHighlightType.GENERIC_ERROR else ProblemHighlightType.WARNING

        for ((index, dir) in dirs.withIndex()) {
            val files = mutableMapOf<String, File>()
            pathForRange += if (index == 0) dir else "/$dir"
            pathDirs = Paths.get(pathDirs, dir.toString()).toString()
            for (path in paths) {
                val localPath = Paths.get(Path.of(File.separator + path).toString(), pathDirs) ?: continue
                files[localPath.toString()] = localPath.toFile()
            }

            val isFindDirectory = files.filter { it.value.isDirectory }.isNotEmpty()
            val indexOf = text.indexOf(pathForRange)
            if (!isFindDirectory && indexOf != -1) {
                val startOffset = indexOf + pathForRange.length
                val curTextRange = TextRange(startOffset - dir.toString().length, startOffset)
                    .shiftRight(range.startOffset)
                val path = files.asSequence()
                    .filter { !it.value.isDirectory }
                    .firstOrNull()?.key ?: continue

                problems += manager.createProblemDescriptor(
                    element,
                    curTextRange,
                    SpringCoreBundle.message("esprito.spring.inspection.directory.resolve.error.message", dir),
                    highlightType,
                    isOnTheFly,
                    ResourceFileQuickFix(path, dir.toString(), true, typeFile)
                )
            }
        }
        return problems
    }

    private fun getSubPath(text: String, fileName: String): String {
        var textWithout = text.substringAfter(SpringProperties.PREFIX_CLASSPATH)
        textWithout = textWithout.substringAfter(SpringProperties.PREFIX_FILE)
        return textWithout.substringBefore(fileName)
    }
}
