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
import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.statistic.StatisticActionId.GENERATE_POST_CONSTRUCT
import com.explyt.spring.core.statistic.StatisticActionId.GENERATE_PRE_DESTROY
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.util.KotlinMethodGenerateUtils
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

class PostConstructKotlinGenerateAction : BaseGenerateAction(GenerateMethodHandler()) {
    init {
        getTemplatePresentation().text = message("explyt.spring.action.post.construct.generate")
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        return KotlinMethodGenerateUtils.isValidForFile(project, editor, file)
        { SpringCoreUtil.isSpringProject(project) }
    }
}

class PreDestroyKotlinGenerateAction : BaseGenerateAction(GenerateMethodHandler(false)) {
    init {
        getTemplatePresentation().text = message("explyt.spring.action.pre.destroy")
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        return KotlinMethodGenerateUtils.isValidForFile(project, editor, file)
        { SpringCoreUtil.isSpringProject(project) }
    }
}

private class GenerateMethodHandler(val postConstruct: Boolean = true) : CodeInsightActionHandler {
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val actionId = if (postConstruct) GENERATE_POST_CONSTRUCT else GENERATE_PRE_DESTROY
        StatisticService.getInstance().addActionUsage(actionId)
        val module = ModuleUtil.findModuleForPsiElement(file) ?: return
        val targetClass = KotlinMethodGenerateUtils.getTargetClass(editor, file) ?: return

        runWriteAction {
            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(file) ?: return@runWriteAction
            PsiDocumentManager.getInstance(file.project).commitDocument(document)


            val offsetToInsert = KotlinMethodGenerateUtils.findOffsetToInsertMethod(editor, file, targetClass)
            val template = getTemplate(module) ?: return@runWriteAction

            editor.caretModel.moveToOffset(offsetToInsert)

            KotlinMethodGenerateUtils.startTemplate(project, editor, template)
        }
    }

    private fun getTemplate(module: Module): Template? {
        val template = TemplateManager.getInstance(module.project).createTemplate("", "")
        val annotationFqn = getAnnotationFqn(module)
        template.addTextSegment("@$annotationFqn fun ")
        val methodName = StringUtil.decapitalize(annotationFqn.substringAfterLast("."))
        val nameExpr = ConstantNode(methodName)
        template.addVariable("name", nameExpr, nameExpr, true)
        template.addTextSegment("() {\n TODO(\"Not yet implemented\")\n }\n")

        template.setToIndent(true)
        template.isToReformat = true
        template.isToShortenLongNames = true
        return template
    }

    private fun getAnnotationFqn(module: Module): String {
        return if (postConstruct) {
            if (searchForLibraryClass(module, SpringCoreClasses.POST_CONSTRUCT_J) != null)
                SpringCoreClasses.POST_CONSTRUCT_J else SpringCoreClasses.POST_CONSTRUCT_X
        } else {
            if (searchForLibraryClass(module, SpringCoreClasses.PRE_DESTROY_J) != null)
                SpringCoreClasses.PRE_DESTROY_J else SpringCoreClasses.PRE_DESTROY_X
        }
    }
}