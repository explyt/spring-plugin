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
import com.explyt.util.ExplytPsiUtil.isNonPrivate
import com.explyt.util.JavaMethodGenerateUtils
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.intellij.lang.annotations.Language
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElement

class EntityEqAndHashCodeJavaGenerateAction : BaseGenerateAction(EqAndHashCodeMethodHandler()) {
    init {
        getTemplatePresentation().text = JpaBundle.message("explyt.jpa.action.eq.hashcode.title")
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        return file.isWritable && super.isValidForFile(project, editor, file)
                && file.fileType == JavaFileType.INSTANCE
                && JpaService.getInstance(project).isJpaEntity(file)
    }

    fun getNearTargetClass(editor: Editor?, file: PsiFile?): PsiClass? {
        return super.getTargetClass(editor, file)
    }
}

private class EqAndHashCodeMethodHandler() : CodeInsightActionHandler {
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val targetClass = EntityEqAndHashCodeJavaGenerateAction().getNearTargetClass(editor, file) ?: return

        runWriteAction {
            val uClass = targetClass.toUElement() as? UClass ?: return@runWriteAction
            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(file) ?: return@runWriteAction
            PsiDocumentManager.getInstance(file.project).commitDocument(document)


            val offsetToInsert = JavaMethodGenerateUtils.findOffsetToInsertMethod(editor, file, targetClass)
            val template = getTemplate(uClass) ?: return@runWriteAction

            editor.caretModel.moveToOffset(offsetToInsert)

            JavaMethodGenerateUtils.startTemplate(project, editor, template)
        }
    }

    private fun getTemplate(uClass: UClass): Template? {
        val targetClass = uClass.javaPsi
        val template = TemplateManager.getInstance(targetClass.project).createTemplate("", "")
        val idExpression = getIdExpression(uClass) ?: return null
        @Language("java") val eqAndHashCodeTemplate = """
            @Override
    public final boolean equals(Object o) {
        if (!(o instanceof ${targetClass.name} that)) return false;
        return $idExpression != null && $idExpression.equals(that.$idExpression);
    }

    @Override
    public final int hashCode() {
        return ${targetClass.name}.class.hashCode();
    }
        """.trimIndent()
        template.addTextSegment(eqAndHashCodeTemplate)
        template.setToIndent(true)
        template.isToReformat = true
        template.isToShortenLongNames = true
        return template
    }

    private fun getIdExpression(uClass: UClass): String? {
        val psiClass = uClass.javaPsi
        val methodIdExpression = psiClass.allMethods
            .filter { it.name.startsWith("get") }
            .firstOrNull { it.annotations.any { annotation -> annotation.qualifiedName?.endsWith(".Id") == true } }
            ?.let { it.name + "()" }
        if (methodIdExpression != null) return methodIdExpression
        val idField = psiClass.allFields
            .firstOrNull { it.annotations.any { annotation -> annotation.qualifiedName?.endsWith(".Id") == true } }
            ?: return null
        if (idField.isNonPrivate) return idField.name
        return psiClass.allMethods.firstOrNull { it.name.equals("get${idField.name}", true) }?.let { it.name + "()" }
    }
}