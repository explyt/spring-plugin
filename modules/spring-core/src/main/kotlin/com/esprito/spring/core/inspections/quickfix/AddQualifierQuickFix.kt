package com.esprito.spring.core.inspections.quickfix

import com.esprito.spring.core.SpringCoreBundle
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.codeStyle.JavaCodeStyleManager


/**
 * This class provides a solution to inspection
 */
class AddQualifierQuickFix(val annotation: String, element: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    override fun getFamilyName(): String =
        SpringCoreBundle.message("esprito.spring.inspection.bean.autowired.use.quickfix")

    override fun getText(): String = familyName
    override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
        if (!ApplicationManager.getApplication().isWriteAccessAllowed) return

        if (startElement.context !is PsiModifierListOwner) return
        val psiElement = startElement.context as PsiModifierListOwner
        val containingFile = psiElement.containingFile

        WriteCommandAction.runWriteCommandAction(project, "Annotate with @Qualifier", null, {
            val psiAnnotation = psiElement.modifierList?.addAnnotation("$annotation(\"\")")
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiElement)

            if (editor != null) {
                psiAnnotation?.textRange?.endOffset?.minus(2)?.let { editor.caretModel.moveToOffset(it) }
                AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
            }
        }, containingFile)
    }
}
