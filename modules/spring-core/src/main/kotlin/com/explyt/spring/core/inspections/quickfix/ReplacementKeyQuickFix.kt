/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.util.RenameUtil
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtPsiFactory

class ReplacementKeyQuickFix(val key: String, element: PsiElement) :
    LocalQuickFixAndIntentionActionOnPsiElement(element) {
    override fun getFamilyName(): String =
        SpringCoreBundle.message("explyt.spring.inspection.properties.quick.fix.replacement", key)

    override fun getText(): String = familyName

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        if (startElement !is PropertyImpl) return
        val oldKey = startElement.key ?: return
        if (oldKey == key) return
        val containingFile = startElement.context?.containingFile

        // Collect the usages before the key is renamed: afterwards `${oldKey}` references no longer resolve to it.
        val usages = ReferencesSearch.search(startElement).findAll().toList()

        WriteCommandAction.runWriteCommandAction(project, "Replace Key", null, {
            if (!startElement.isValid) return@runWriteCommandAction
            // Rename through PSI so the fix also works without an editor (batch / "Fix all" inspection runs).
            startElement.setName(key)

            renameUsages(project, usages, key)
            RenameUtil.renameSameProperty(project, startElement, oldKey, key)
        }, containingFile)
    }

    private fun renameUsages(project: Project, usages: List<PsiReference>, newKey: String) {
        if (usages.isEmpty()) return

        for (usage in usages) {
            val usageElement = usage.element
            if (!usageElement.isValid) continue
            val oldText = usageElement.text.substringAfter("{").substringBefore("}").substringBefore(":")
            val newText = usageElement.text.replace(oldText, newKey)
            val newElement = if (usageElement.language == KotlinLanguage.INSTANCE) {
                val factory = KtPsiFactory(usageElement.project)
                factory.createExpression(newText)
            } else {
                PsiElementFactory.getInstance(usageElement.project)
                    .createExpressionFromText(newText, usageElement.context)
            }
            WriteCommandAction.runWriteCommandAction(project) {
                usageElement.replace(newElement)
            }
        }
    }
}
