/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.util

import com.intellij.codeInsight.daemon.impl.quickfix.CreateFromUsageUtils
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.startOffset
import org.jetbrains.kotlin.psi.psiUtil.endOffset

object JavaMethodGenerateUtils {

    fun findOffsetToInsertMethod(editor: Editor, file: PsiFile, targetClass: PsiClass): Int {
        var result = editor.caretModel.offset
        val psiMethod = PsiTreeUtil.findElementOfClassAtOffset(file, result - 1, PsiMethod::class.java, false)
        if (psiMethod != null) {
            return psiMethod.startOffset
        }
        val psiField = PsiTreeUtil.findElementOfClassAtOffset(file, result - 1, PsiField::class.java, false)
        if (psiField != null) {
            return psiField.endOffset
        }

        var classAtCursor = PsiTreeUtil.getParentOfType(file.findElementAt(result), PsiClass::class.java, false)
        if (classAtCursor == targetClass) {
            return result
        }

        while (classAtCursor != null && classAtCursor.parent !is PsiFile) {
            result = classAtCursor.textRange.endOffset
            classAtCursor = PsiTreeUtil.getParentOfType(classAtCursor, PsiClass::class.java)
        }

        return result
    }

    fun startTemplate(project: Project, editor: Editor, template: Template) {
        val adapter = object : TemplateEditingAdapter() {
            override fun templateFinished(template: Template, brokenOff: Boolean) {
                ApplicationManager.getApplication().runWriteAction {
                    PsiDocumentManager.getInstance(project).commitDocument(editor.document)
                    PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
                        ?.let { findPsiMethod(it, editor) }
                        ?.let { PsiTreeUtil.getParentOfType(it, PsiMethod::class.java, false) }
                        ?.let { CreateFromUsageUtils.setupEditor(it, editor) }
                }
            }
        }
        TemplateManager.getInstance(project).startTemplate(editor, template, adapter)
    }

    private fun findPsiMethod(it: PsiFile, editor: Editor): PsiMethod? {
        for (i in 2..20) {
            val psiMethod = PsiTreeUtil.findElementOfClassAtOffset(
                it, editor.caretModel.offset - i, PsiMethod::class.java, false
            )
            if (psiMethod != null) return psiMethod
        }
        return null
    }
}