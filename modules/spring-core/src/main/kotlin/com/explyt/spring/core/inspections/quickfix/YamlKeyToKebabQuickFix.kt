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
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl

class YamlKeyToKebabQuickFix(element: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
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

        WriteCommandAction.runWriteCommandAction(project, "Replace Key", null, {
            if (editor != null) {
                var current: YAMLKeyValueImpl? = startElement
                var isRenamed = false
                while (current != null) {
                    val key = current.key
                    if (key != null) {
                        val textToUpdate = key.text
                        val startOffset = key.textRange.startOffset
                        val end = startOffset + textToUpdate.length
                        val newKey = toKebabCase(textToUpdate)

                        if (key.text != newKey) {
                            isRenamed = true
                            editor.caretModel.moveToOffset(startOffset)
                            editor.selectionModel.setSelection(startOffset, end)
                            EditorModificationUtil.insertStringAtCaret(editor, newKey)
                        }
                    }
                    current = current.parent?.parent as? YAMLKeyValueImpl
                }
                if (isRenamed) {
                    val fullName = YAMLUtil.getConfigFullName(startElement)
                    renameUsages(startElement, toKebabCase(fullName))
                    RenameUtil.renameSameProperty(project, startElement, fullName, toKebabCase(fullName))
                }
            }
        }, containingFile)

    }

    private fun renameUsages(elementToRename: YAMLKeyValueImpl, newFullName: String) {
        val usages = ReferencesSearch.search(elementToRename).findAll().toList()
        if (usages.isEmpty()) return

        val project = elementToRename.project
        for (usage in usages) {
            val usageElement = usage.element
            val oldText = usageElement.text.substringAfter("{").substringBefore("}").substringBefore(":")
            val newText = usageElement.text.replace(oldText, newFullName)
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
