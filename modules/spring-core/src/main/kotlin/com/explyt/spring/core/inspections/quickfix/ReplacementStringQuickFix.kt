/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinLanguage

class ReplacementStringQuickFix(
    private val oldValue: String,
    private val newValue: String,
    element: PsiElement,
    private val range: TextRange? = null
) :
    LocalQuickFixAndIntentionActionOnPsiElement(element) {

    override fun getFamilyName(): String = SpringCoreBundle
        .message("explyt.spring.inspection.value.replacement.fix", oldValue, newValue)

    override fun getText(): String = familyName

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        if (!ApplicationManager.getApplication().isWriteAccessAllowed) return

        val containingFile = startElement.context?.containingFile

        WriteCommandAction.runWriteCommandAction(project, "Replacement String", null, {
            if (editor != null) {
                if (!ElementManipulators.getValueText(startElement).contains(oldValue)) return@runWriteCommandAction
                val valueTextRange = range ?: ElementManipulators.getValueTextRange(startElement)
                val startOffset = startElement.textRange.startOffset + valueTextRange.startOffset
                val end = startOffset + oldValue.length
                editor.caretModel.moveToOffset(startOffset)
                editor.selectionModel.setSelection(startOffset, end)
                EditorModificationUtil.deleteSelectedText(editor)
                val valueFinal = if (startElement.language == KotlinLanguage.INSTANCE) "\\" + newValue else newValue
                EditorModificationUtil.insertStringAtCaret(editor, valueFinal)
            }
        }, containingFile)

    }
}
