/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.action

import com.explyt.base.LibraryClassCache.searchForLibraryClass
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.statistic.StatisticActionId.GENERATE_POST_CONSTRUCT
import com.explyt.spring.core.statistic.StatisticActionId.GENERATE_PRE_DESTROY
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.util.JavaMethodGenerateUtils
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.SyntheticElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Callable


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

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val actionId = if (postConstruct) GENERATE_POST_CONSTRUCT else GENERATE_PRE_DESTROY
        StatisticService.getInstance().addActionUsage(actionId)
        val caretOffset = editor.caretModel.offset
        val filePointer = SmartPointerManager.createPointer(file)

        ReadAction.nonBlocking(Callable { resolveInsertion(filePointer, caretOffset) })
            .inSmartMode(project)
            .expireWhen { project.isDisposed || editor.isDisposed }
            .finishOnUiThread(ModalityState.defaultModalityState()) { insertion ->
                insertion?.let { insertTemplate(project, editor, it) }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun resolveInsertion(filePointer: SmartPsiElementPointer<PsiFile>, caretOffset: Int): Insertion? {
        val file = filePointer.element ?: return null
        val module = ModuleUtil.findModuleForPsiElement(file) ?: return null
        val targetClass = PsiTreeUtil.getParentOfType(file.findElementAt(caretOffset), PsiClass::class.java)
            ?.takeUnless { it is SyntheticElement }
            ?: return null
        return Insertion(filePointer, SmartPointerManager.createPointer(targetClass), getAnnotationFqn(module))
    }

    private fun insertTemplate(project: Project, editor: Editor, insertion: Insertion) {
        val file = insertion.file.element ?: return
        if (!FileModificationService.getInstance().preparePsiElementForWrite(file)) return

        WriteCommandAction.writeCommandAction(project, file).withName(commandName()).run<RuntimeException> {
            val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return@run
            PsiDocumentManager.getInstance(project).commitDocument(document)
            val targetClass = insertion.targetClass.element ?: return@run

            val offsetToInsert = JavaMethodGenerateUtils.findOffsetToInsertMethod(editor, file, targetClass)
            val template = getTemplate(project, insertion.annotationFqn)

            editor.caretModel.moveToOffset(offsetToInsert)

            JavaMethodGenerateUtils.startTemplate(project, editor, template)
        }
    }

    private fun commandName(): String = SpringCoreBundle.message(
        if (postConstruct) "explyt.spring.action.post.construct.generate" else "explyt.spring.action.pre.destroy"
    )

    private fun getTemplate(project: Project, annotationFqn: String): Template {
        val template = TemplateManager.getInstance(project).createTemplate("", "")
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


    private fun getAnnotationFqn(module: Module): String {
        return if (postConstruct) {
            if (searchForLibraryClass(module, SpringCoreClasses.POST_CONSTRUCT_X) != null)
                SpringCoreClasses.POST_CONSTRUCT_X else SpringCoreClasses.POST_CONSTRUCT_J
        } else {
            if (searchForLibraryClass(module, SpringCoreClasses.PRE_DESTROY_X) != null)
                SpringCoreClasses.PRE_DESTROY_X else SpringCoreClasses.PRE_DESTROY_J
        }
    }

    private class Insertion(
        val file: SmartPsiElementPointer<PsiFile>,
        val targetClass: SmartPsiElementPointer<PsiClass>,
        val annotationFqn: String,
    )
}

object JavaCoreMethodGenerateUtils {

    fun getNearTargetClass(editor: Editor?, file: PsiFile?): PsiClass? {
        return PostConstructJavaGenerateAction().getNearTargetClass(editor, file)
    }
}
