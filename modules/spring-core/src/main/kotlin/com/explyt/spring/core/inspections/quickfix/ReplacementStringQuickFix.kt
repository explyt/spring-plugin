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
