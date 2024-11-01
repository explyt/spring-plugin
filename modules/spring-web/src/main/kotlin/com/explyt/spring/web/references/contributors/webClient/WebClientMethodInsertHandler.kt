package com.explyt.spring.web.references.contributors.webClient

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.EditorModificationUtilEx
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import org.jetbrains.kotlin.idea.base.psi.imports.addImport
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

class WebClientMethodInsertHandler(private val type: String, private val toImport: Set<FqName> = setOf()) :
    InsertHandler<LookupElement> {

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        EditorModificationUtilEx.insertStringAtCaret(context.editor, type)
        PsiDocumentManager.getInstance(context.project).commitDocument(context.document)

        val ktFile = context.file as? KtFile
        if (ktFile != null) {
            for (fqName in toImport) {
                ktFile.addImport(fqName)
            }
            PsiDocumentManager.getInstance(context.project).commitDocument(context.document)
            OptimizeImportsProcessor(context.project, context.file).runWithoutProgress()
            return
        }

        val underCursor = context.file.findElementAt(context.tailOffset)?.parent ?: return
        JavaCodeStyleManager.getInstance(context.project).shortenClassReferences(underCursor)
    }

}
