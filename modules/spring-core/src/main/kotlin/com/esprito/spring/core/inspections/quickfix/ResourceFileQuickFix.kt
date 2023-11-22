package com.esprito.spring.core.inspections.quickfix

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringProperties
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.lang.Language
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.findDirectory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


class ResourceFileQuickFix(
    val path: String,
    private val nameObject: String,
    private val isDir: Boolean,
    private val typeFile: String = "Properties",
) : LocalQuickFix {
    override fun getFamilyName(): String = SpringCoreBundle.message(
        "esprito.spring.inspection.property.source.prefix.file.quick.fix",
        if (isDir) "directory" else "file",
        nameObject
    )

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        if (isDir) {
            createDirectory()
        } else {
            createFile(project)
        }
    }

    private fun createDirectory() {
        val file = File(path)
        if (!file.isDirectory) {
            Files.createDirectories(Paths.get(path))
        }
    }

    private fun createFile(project: Project) {
        WriteCommandAction.runWriteCommandAction(project) {
            createDirectory()

            val lang = Language.findLanguageByID(typeFile) ?: return@runWriteCommandAction
            val directory = ProjectRootManager.getInstance(project).contentRoots
                .map { it.findDirectory(path) }
                .firstOrNull() ?: return@runWriteCommandAction
            val psiDirector = PsiManager.getInstance(project).findDirectory(directory) ?: return@runWriteCommandAction

            val text = getTemplateTextForFile(project, typeFile)
            val file = PsiFileFactory.getInstance(project).createFileFromText(nameObject, lang, text)
            psiDirector.add(file)

            val document =
                FileDocumentManager.getInstance().getDocument(file.virtualFile) ?: return@runWriteCommandAction
            FileDocumentManager.getInstance().saveDocument(document)
            CodeStyleManager.getInstance(project).reformat(file)

            val fileDescriptor = OpenFileDescriptor(project, file.virtualFile)
            FileEditorManager.getInstance(project).openEditor(fileDescriptor, true)
        }
    }

    private fun getTemplateTextForFile(project: Project, typeFile: String): String {
        return when (typeFile) {
            "XML" -> {
                val template =
                    FileTemplateManager.getInstance(project).getDefaultTemplate(SpringProperties.SPRING_XML_TEMPLATE)
                template.text
            }
            else -> ""
        }
   }
}

