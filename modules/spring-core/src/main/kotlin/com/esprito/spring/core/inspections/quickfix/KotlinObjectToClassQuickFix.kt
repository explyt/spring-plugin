package com.esprito.spring.core.inspections.quickfix;

import com.esprito.spring.core.SpringCoreBundle
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class KotlinObjectToClassQuickFix(element: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    override fun getFamilyName(): String = SpringCoreBundle.message("esprito.spring.inspection.kotlin.object.fix")

    override fun getText(): String = familyName

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        if (!ApplicationManager.getApplication().isWriteAccessAllowed) return

        val objectKeywordPsi = (startElement as? KtObjectDeclaration)?.getObjectKeyword() ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            if (editor != null) {
                val startOffset = objectKeywordPsi.textRange.startOffset
                val end = objectKeywordPsi.endOffset
                editor.caretModel.moveToOffset(startOffset)
                editor.selectionModel.setSelection(startOffset, end)
                EditorModificationUtil.deleteSelectedText(editor)
                EditorModificationUtil.insertStringAtCaret(editor, "class")
            }
        }

    }
}
