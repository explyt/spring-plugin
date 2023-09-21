package com.esprito.spring.core.inspections.quickfix

import com.esprito.spring.core.SpringCoreBundle
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager

/**
 * This class provides a solution to inspection
 */
class AddQualifierQuickFix(val annotation: String, val element: PsiElement) : LocalQuickFixOnPsiElement(element) {

    override fun getFamilyName(): String = SpringCoreBundle.message("esprito.spring.inspection.bean.autowired.use.quickfix")

    override fun getText(): String = familyName
    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val psiElement = startElement.context as PsiModifierListOwner
        val containingFile = psiElement.containingFile

        WriteCommandAction.runWriteCommandAction(project, "Annotate with @Qualifier", null, Runnable {
            psiElement.modifierList?.addAnnotation("$annotation(\"\")")
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiElement)
        }, containingFile)
    }
}
