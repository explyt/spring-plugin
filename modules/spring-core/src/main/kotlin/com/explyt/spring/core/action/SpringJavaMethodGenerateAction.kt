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

package com.explyt.spring.core.action

import com.explyt.base.LibraryClassCache.searchForLibraryClass
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.daemon.impl.quickfix.CreateFromUsageUtils
import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.startOffset
import org.jetbrains.kotlin.psi.psiUtil.endOffset


class PostConstructJavaGenerateAction : BaseGenerateAction(PostConstructMethodHandler()) {
    init {
        getTemplatePresentation().text = SpringCoreBundle.message("explyt.spring.action.post.construct.generate")
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        return file.isWritable && super.isValidForFile(project, editor, file) && SpringCoreUtil.isSpringProject(project)
    }

    fun getNearTargetClass(editor: Editor?, file: PsiFile?): PsiClass? {
        return super.getTargetClass(editor, file)
    }
}

class PreDestroyJavaGenerateAction : BaseGenerateAction(PostConstructMethodHandler(false)) {
    init {
        getTemplatePresentation().text = SpringCoreBundle.message("explyt.spring.action.pre.destroy")
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        return file.isWritable && super.isValidForFile(project, editor, file) && SpringCoreUtil.isSpringProject(project)
    }
}

private class PostConstructMethodHandler(val postConstruct: Boolean = true) : CodeInsightActionHandler {
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val module = ModuleUtil.findModuleForPsiElement(file) ?: return
        val targetClass = PostConstructJavaGenerateAction().getNearTargetClass(editor, file) ?: return

        runWriteAction {
            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(file) ?: return@runWriteAction
            PsiDocumentManager.getInstance(file.project).commitDocument(document)


            val offsetToInsert = findOffsetToInsertMethodTo(editor, file, targetClass)
            val template = getTemplate(module) ?: return@runWriteAction

            editor.caretModel.moveToOffset(offsetToInsert)

            startTemplate(project, editor, template)
        }
    }

    private fun startTemplate(project: Project, editor: Editor, template: Template) {
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

    private fun getTemplate(module: Module): Template? {
        val template = TemplateManager.getInstance(module.project).createTemplate("", "")
        val annotationFqn = getAnnotationFqn(module)
        template.addTextSegment("@$annotationFqn public void ")
        val methodName = StringUtil.decapitalize(annotationFqn.substringAfterLast("."))
        val nameExpr = ConstantNode(methodName)
        template.addVariable("name", nameExpr, nameExpr, true)
        template.addTextSegment("() {\n  }\n")

        template.setToIndent(true)
        template.isToReformat = true
        template.isToShortenLongNames = true
        return template
    }

    private fun findOffsetToInsertMethodTo(editor: Editor, file: PsiFile, targetClass: PsiClass): Int {
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

    private fun getAnnotationFqn(module: Module): String {
        return if (postConstruct) {
            if (searchForLibraryClass(module, SpringCoreClasses.POST_CONSTRUCT_X) != null)
                SpringCoreClasses.POST_CONSTRUCT_X else SpringCoreClasses.POST_CONSTRUCT_J
        } else {
            if (searchForLibraryClass(module, SpringCoreClasses.PRE_DESTROY_X) != null)
                SpringCoreClasses.PRE_DESTROY_X else SpringCoreClasses.PRE_DESTROY_J
        }
    }
}