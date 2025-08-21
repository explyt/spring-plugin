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

package com.explyt.jpa.action

import com.explyt.jpa.JpaBundle
import com.explyt.jpa.service.JpaService
import com.explyt.util.KotlinMethodGenerateUtils
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElement

class EntityEqAndHashCodeKotlinGenerateAction : BaseGenerateAction(EqAndHashCodeMethodKotlinHandler()) {
    init {
        getTemplatePresentation().text = JpaBundle.message("explyt.jpa.action.eq.hashcode.title")
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        return file.fileType == KotlinFileType.INSTANCE
                && KotlinMethodGenerateUtils.isValidForFile(project, editor, file)
        { JpaService.getInstance(project).isJpaEntity(file) }
    }
}

private class EqAndHashCodeMethodKotlinHandler() : CodeInsightActionHandler {
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val module = ModuleUtil.findModuleForPsiElement(file) ?: return
        val targetClass = KotlinMethodGenerateUtils.getTargetClass(editor, file) ?: return

        runWriteAction {
            val uClass = targetClass.toUElement() as? UClass ?: return@runWriteAction
            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(file) ?: return@runWriteAction
            PsiDocumentManager.getInstance(file.project).commitDocument(document)


            val offsetToInsert = KotlinMethodGenerateUtils.findOffsetToInsertMethod(editor, file, targetClass)
            val template = getTemplate(module, uClass) ?: return@runWriteAction

            editor.caretModel.moveToOffset(offsetToInsert)

            KotlinMethodGenerateUtils.startTemplate(project, editor, template)
        }
    }

    private fun getTemplate(module: Module, uClass: UClass): Template? {
        val targetClass = uClass.javaPsi
        val template = TemplateManager.getInstance(module.project).createTemplate("", "")
        val idExpression = getIdExpression(uClass) ?: return null
        @Language("kotlin") val eqAndHashCodeTemplate = """
                override fun equals(o: Any?): Boolean {
        if (o !is ${targetClass.name}) return false
        return $idExpression != null && $idExpression == o.$idExpression
    }

    override fun hashCode(): Int {
        return ${targetClass.name}::class.java.hashCode()
    }
        """.trimIndent()
        template.addTextSegment(eqAndHashCodeTemplate)
        template.setToIndent(true)
        template.isToReformat = true
        template.isToShortenLongNames = true
        return template
    }

    private fun getIdExpression(uClass: UClass): String? {
        return uClass.javaPsi.allFields
            .firstOrNull { it.annotations.any { annotation -> annotation.qualifiedName?.endsWith(".Id") == true } }
            ?.name
    }
}