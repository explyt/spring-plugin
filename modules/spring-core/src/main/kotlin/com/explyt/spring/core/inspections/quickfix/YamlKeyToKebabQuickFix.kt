package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.util.PropertyUtil.toKebabCase
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
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

        WriteCommandAction.runWriteCommandAction(project, "Replace key", null, {
            if (editor != null) {
                var current: YAMLKeyValueImpl? = startElement

                while (current != null) {
                    val key = current.key
                    if (key != null && key.text.contains('_')) {
                        val textToUpdate = key.text
                        val startOffset = key.textRange.startOffset
                        val end = startOffset + textToUpdate.length
                        editor.caretModel.moveToOffset(startOffset)
                        editor.selectionModel.setSelection(startOffset, end)
                        EditorModificationUtil.insertStringAtCaret(editor, toKebabCase(textToUpdate))
                    }
                    current = current.parent?.parent as? YAMLKeyValueImpl
                }
            }
        }, containingFile)

    }
}
