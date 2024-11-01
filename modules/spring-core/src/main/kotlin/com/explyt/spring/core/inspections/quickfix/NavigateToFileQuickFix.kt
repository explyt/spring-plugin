package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle.message
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.codeInsight.highlighting.HighlightUsagesHandler
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.find.FindModel.initStringToFind
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColors.SEARCH_RESULT_ATTRIBUTES
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class NavigateToFileQuickFix(val file: VirtualFile, val search: String? = null) : LocalQuickFix {
    private val fileName: String = file.name

    override fun startInWriteAction(): Boolean = false

    override fun getFamilyName(): String = message("explyt.spring.quick.fix.navigate", fileName)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(project)
                .openTextEditor(OpenFileDescriptor(project, file), true) ?: return@invokeLater
            if (search == null) return@invokeLater

            val findModel = FindModel().apply { initStringToFind(this, search) }
            val findResult = FindManager.getInstance(project)
                .findString(editor.document.charsSequence, 0, findModel, file)
            val highlightManager = HighlightManager.getInstance(project)
            HighlightUsagesHandler.highlightRanges(
                highlightManager, editor, SEARCH_RESULT_ATTRIBUTES, false, listOf(findResult)
            )
        }
    }
}