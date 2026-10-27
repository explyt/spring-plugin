/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.util.RenameUtil
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.ReferencesSearch

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

        // The platform renders the Alt+Enter preview by invoking the fix on a file copy inside a read action, where
        // starting a write action deadlocks. In that mode the copy is already modifiable, so only the key of the
        // shown file is renamed: `${...}` usages and same-key properties live in other real files, which a preview
        // must never touch.
        if (IntentionPreviewUtils.isIntentionPreviewActive()) {
            startElement.setName(key)
            return
        }

        val containingFile = startElement.context?.containingFile

        // Collect the usages before the key is renamed: afterwards `${oldKey}` references no longer resolve to it.
        val usages = ReferencesSearch.search(startElement).findAll().toList()

        WriteCommandAction.runWriteCommandAction(project, "Replace Key", null, {
            if (!startElement.isValid) return@runWriteCommandAction
            // Rename through PSI so the fix also works without an editor (batch / "Fix all" inspection runs).
            startElement.setName(key)

            QuickFixUsageRenamer.renameKeyInUsages(project, usages, key)
            RenameUtil.renameSameProperty(project, startElement, oldKey, key)
        }, containingFile)
    }
}
