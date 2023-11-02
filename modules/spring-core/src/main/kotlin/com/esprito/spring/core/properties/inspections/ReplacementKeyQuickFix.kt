package com.esprito.spring.core.properties.inspections;

import com.esprito.spring.core.SpringCoreBundle
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile

class ReplacementKeyQuickFix(val key: String, element: PsiElement)  : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    override fun getFamilyName(): String = SpringCoreBundle.message("esprito.spring.inspection.properties.replacement.quick.fix", key)

    override fun getText(): String = familyName

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        if (!ApplicationManager.getApplication().isWriteAccessAllowed) return

        if (startElement !is PropertyImpl) return
        val containingFile = startElement.context?.containingFile

        WriteCommandAction.runWriteCommandAction(project, "Replace key", null, {
            if (editor != null) {
                val startOffset = startElement.textRange.startOffset
                val end = startOffset + startElement.text.substringBefore("=").length
                editor.caretModel.moveToOffset(startOffset)
                editor.selectionModel.setSelection(startOffset, end)
                EditorModificationUtil.deleteSelectedText(editor)
                EditorModificationUtil.insertStringAtCaret(editor, key)
            }
        }, containingFile)

    }
}
