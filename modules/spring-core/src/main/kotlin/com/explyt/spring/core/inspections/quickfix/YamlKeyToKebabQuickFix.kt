/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.statistic.StatisticActionId.PREVIEW_YAML_SWITCH_KEY_TO_KEBAB_CASE
import com.explyt.spring.core.statistic.StatisticActionId.QUICK_FIX_YAML_SWITCH_KEY_TO_KEBAB_CASE
import com.explyt.spring.core.statistic.StatisticUtil.registerActionUsage
import com.explyt.spring.core.util.PropertyUtil.toKebabCase
import com.explyt.spring.core.util.RenameUtil
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl

class YamlKeyToKebabQuickFix(element: PsiElement, private val batchSafe: Boolean? = null) :
    LocalQuickFixAndIntentionActionOnPsiElement(element) {
    override fun availableInBatchMode() = batchSafe != false

    override fun getFamilyName(): String =
        SpringCoreBundle.message("explyt.spring.inspection.properties.key.yaml.fix.case")

    override fun getText(): String = familyName

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        if (!ApplicationManager.getApplication().isWriteAccessAllowed) return

        if (startElement !is YAMLKeyValueImpl) return
        val containingFile = startElement.context?.containingFile

        editor.registerActionUsage(
            QUICK_FIX_YAML_SWITCH_KEY_TO_KEBAB_CASE,
            PREVIEW_YAML_SWITCH_KEY_TO_KEBAB_CASE
        )

        val fullName = YAMLUtil.getConfigFullName(startElement)
        val newFullName = toKebabCase(fullName)
        // Collected before the key is renamed: afterwards a `${...}` placeholder no longer resolves to it.
        val localOnly = batchSafe == true && editor == null
        val scope = if (localOnly) LocalSearchScope(startElement.containingFile) else GlobalSearchScope.allScope(project)
        val usages = ReferencesSearch.search(startElement, scope).findAll().toList()

        WriteCommandAction.runWriteCommandAction(project, "Replace Key", null, {
            if (!renameKeySegments(project, startElement)) return@runWriteCommandAction

            QuickFixUsageRenamer.renameKeyInUsages(project, usages, newFullName)
            if (!localOnly) RenameUtil.renameSameProperty(project, startElement, fullName, newFullName)
        }, containingFile)
    }

    /**
     * Renames the flagged key and every ancestor segment, returning whether anything changed.
     *
     * The segments are rewritten through the document rather than by typing into the editor, so the fix also works
     * in batch mode (`Fix all`, `Code | Inspect Code`), where no editor exists. Deepest segment first: rewriting an
     * ancestor first would shift the offsets of the keys nested below it.
     */
    private fun renameKeySegments(project: Project, startElement: YAMLKeyValueImpl): Boolean {
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(startElement.containingFile) ?: return false

        // The enclosing key, not the grandparent: under a list the next key sits beyond a sequence item and sequence.
        val renames = keyPath(startElement)
            .mapNotNull { it.key }
            .map { it.textRange to toKebabCase(it.text) }
            .filter { (range, newKey) -> document.getText(range) != newKey }
            .toList()
        if (renames.isEmpty()) return false

        documentManager.doPostponedOperationsAndUnblockDocument(document)
        for ((range, newKey) in renames.sortedByDescending { it.first.startOffset }) {
            document.replaceString(range.startOffset, range.endOffset, newKey)
        }
        documentManager.commitDocument(document)
        return true
    }

    companion object {
        internal fun hasNoStructuralConflict(element: PsiElement): Boolean {
            if (element !is YAMLKeyValueImpl) return true
            return keyPath(element).all { entry ->
                val mapping = entry.parentMapping
                if (mapping?.getKeyValueByKey("<<") != null) return@all false
                val keyText = entry.key?.text ?: return@all true
                val canonical = toKebabCase(keyText)
                if (keyText == canonical) return@all true
                keyText == entry.keyText &&
                        mapping?.keyValues.orEmpty().none { it != entry && toKebabCase(it.keyText) == canonical } &&
                        PsiTreeUtil.findChildrenOfType(entry, YAMLKeyValue::class.java).all {
                            PsiTreeUtil.isAncestor(it, element, false)
                        }
            }
        }

        private fun keyPath(element: YAMLKeyValueImpl): Sequence<YAMLKeyValueImpl> =
            generateSequence(element) { PsiTreeUtil.getParentOfType(it, YAMLKeyValueImpl::class.java) }
    }
}
