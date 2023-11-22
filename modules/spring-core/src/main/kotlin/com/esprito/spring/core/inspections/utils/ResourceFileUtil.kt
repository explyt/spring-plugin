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

object ResourceFileUtil {
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
        val problems = mutableListOf<ProblemDescriptor>()
        val root = ModuleUtil.getContentRootFile(element) ?: return emptyList()

        if (text.startsWith(SpringProperties.PREFIX_FILE)) {
            val textWithoutPrefix = text.substringAfter(SpringProperties.PREFIX_FILE)
            val path = if (textWithoutPrefix.startsWith("/")) "" else root.path

            problems += getProblemsPaths(typeFile, textWithoutPrefix, element, text, path, manager, isOnTheFly)
        }
        return problems
    }

    fun getPathProblemsClasspath(
        typeFile: String,
        text: String,
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): List<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()

        if (text.startsWith(SpringProperties.PREFIX_FILE)) {
            return emptyList()
        }
        val textWithoutPrefix = text.substringAfter(SpringProperties.PREFIX_CLASSPATH)
        val resources = ExternalSystemModule.of(module).resourceRoots

        for (resource in resources) {
            val path = resource.virtualFile.path
            problems += getProblemsPaths(typeFile, textWithoutPrefix, element, text, path, manager, isOnTheFly)
        }

        return problems
    }


    private fun getProblemsPaths(
        typeFile: String,
        textWithoutPrefix: String,
        element: PsiElement,
        text: String,
        path: String,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): List<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()

        val pathList = Path.of(textWithoutPrefix).toList()
        val fileName = pathList.last()
        val dirs = if (pathList.size > 1) pathList.slice(IntRange(0, pathList.size - 2)) else emptyList()

        val range = ElementManipulators.getValueTextRange(element)

        val resultPath = getDirProblems(typeFile, text, dirs, path, range, manager, element, isOnTheFly, problems)
        problems += getFileProblem(typeFile, text, fileName, resultPath, range, manager, element, isOnTheFly)

        return problems
    }

    private fun getFileProblem(
        typeFile: String,
        text: String,
        fileName: Path,
        path: String,
        range: TextRange,
        manager: InspectionManager,
        element: PsiElement,
        isOnTheFly: Boolean
    ): List<ProblemDescriptor> {
        val file = Path.of(path, fileName.toString()).toFile()
        val startOffset = text.indexOf(fileName.toString())
        val highlightType = if (typeFile == "XML") ProblemHighlightType.GENERIC_ERROR else ProblemHighlightType.WARNING

        if (!file.isFile && startOffset != -1) {
            val curTextRange =
                TextRange(startOffset, startOffset + fileName.toString().length).shiftRight(range.startOffset)
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
        typeFile: String,
        text: String,
        dirs: List<Path>,
        path: String,
        range: TextRange,
        manager: InspectionManager,
        element: PsiElement,
        isOnTheFly: Boolean,
        problems: MutableList<ProblemDescriptor>
    ): String {
        var localPath = Path.of(File.separator + path)
        var pathForRange = ""
        val highlightType = if (typeFile == "XML") ProblemHighlightType.GENERIC_ERROR else ProblemHighlightType.WARNING

        dirs.forEachIndexed { index, dir ->
            if (dir.toString() != ".") {
                localPath = Paths.get(localPath.toString(), dir.toString())
                pathForRange += if (index == 0) dir else "/$dir"
                val file = localPath.toFile()
                val indexOf = text.indexOf(pathForRange)

                if (!file.isDirectory && !dir.toString().startsWith("$") && indexOf != -1) {
                    val startOffset = indexOf + pathForRange.length
                    val curTextRange = TextRange(startOffset - dir.toString().length, startOffset)
                        .shiftRight(range.startOffset)

                    problems += manager.createProblemDescriptor(
                        element,
                        curTextRange,
                        SpringCoreBundle.message("esprito.spring.inspection.directory.resolve.error.message", dir),
                        highlightType,
                        isOnTheFly,
                        ResourceFileQuickFix(
                            localPath.toString(),
                            dir.toString(),
                            true,
                            typeFile
                        )
                    )
                }
            }
        }
        return localPath.toString()
    }
}