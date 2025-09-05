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

import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.startOffset
import org.jetbrains.kotlin.idea.base.projectStructure.RootKindFilter
import org.jetbrains.kotlin.idea.base.projectStructure.RootKindMatcher
import org.jetbrains.kotlin.idea.createFromUsage.setupEditorSelection
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import java.util.function.Predicate

object KotlinMethodGenerateUtils {
    fun isValidForFile(project: Project, editor: Editor, file: PsiFile, predicate: Predicate<Project>): Boolean {
        if (!file.isWritable || file !is KtFile || file.isCompiled) return false

        val targetClass = getTargetClass(editor, file) ?: return false
        if (!targetClass.isValid) return false
        val filter = RootKindFilter.projectSources.copy(includeScriptsOutsideSourceRoots = true)
        return RootKindMatcher.matches(targetClass, filter) && predicate.test(project)
    }

    fun getTargetClass(editor: Editor, file: PsiFile): KtClassOrObject? {
        val elementAtCaret = file.findElementAt(editor.caretModel.offset) ?: return null
        return elementAtCaret.parentsWithSelf.filterIsInstance<KtClassOrObject>().firstOrNull { !it.isLocal }
    }

    fun findOffsetToInsertMethod(editor: Editor, file: PsiFile, targetClass: KtClassOrObject?): Int {
        var result = editor.caretModel.offset
        val psiMethod = PsiTreeUtil.findElementOfClassAtOffset(file, result - 1, KtFunction::class.java, false)
        if (psiMethod != null) {
            return psiMethod.startOffset
        }
        val psiField =
            PsiTreeUtil.findElementOfClassAtOffset(file, result - 1, KtVariableDeclaration::class.java, false)
        if (psiField != null) {
            return psiField.endOffset + 1
        }

        var classAtCursor = PsiTreeUtil.getParentOfType(file.findElementAt(result), KtClassOrObject::class.java, false)
        if (classAtCursor == targetClass) {
            return result
        }

        while (classAtCursor != null && classAtCursor.parent !is PsiFile) {
            result = classAtCursor.textRange.endOffset
            classAtCursor = PsiTreeUtil.getParentOfType(classAtCursor, KtClassOrObject::class.java)
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
                        ?.let { setupEditorSelection(editor, it) }
                }
            }
        }
        TemplateManager.getInstance(project).startTemplate(editor, template, adapter)
    }

    private fun findPsiMethod(it: PsiFile, editor: Editor): KtFunction? {
        for (i in 2..20) {
            val psiMethod = PsiTreeUtil.findElementOfClassAtOffset(
                it, editor.caretModel.offset - i, KtFunction::class.java, false
            )
            if (psiMethod != null) return psiMethod
        }
        return null
    }
}