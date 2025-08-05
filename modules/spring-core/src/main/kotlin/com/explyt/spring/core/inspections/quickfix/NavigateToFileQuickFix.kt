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

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle.message
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.codeInsight.highlighting.HighlightUsagesHandler
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.find.FindManager
import com.intellij.find.FindModel
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

            val findModel = FindModel().apply { FindModel.initStringToFind(this, search) }
            val findResult = FindManager.getInstance(project)
                .findString(editor.document.charsSequence, 0, findModel, file)
            val highlightManager = HighlightManager.getInstance(project)
            HighlightUsagesHandler.highlightRanges(
                highlightManager, editor, SEARCH_RESULT_ATTRIBUTES, false, listOf(findResult)
            )
        }
    }
}