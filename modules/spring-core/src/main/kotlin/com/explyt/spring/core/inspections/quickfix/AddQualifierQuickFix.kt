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


class AddQualifierQuickFix(val annotation: String, element: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    override fun getFamilyName(): String =
        SpringCoreBundle.message("explyt.spring.inspection.bean.autowired.use.quickfix")

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
