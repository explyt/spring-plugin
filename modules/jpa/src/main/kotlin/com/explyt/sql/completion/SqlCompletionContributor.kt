/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.sql.completion

import com.explyt.sql.psi.*
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.prevLeaf

class SqlCompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position
        if (position.prevLeaf(skipEmptyElements = true).elementType == SqlTypes.DOT) {
            return
        }

        val aliasDeclaration = position.parentOfType<SqlAliasDeclaration>()
        if (aliasDeclaration != null) {
            if (aliasDeclaration.firstChild?.elementType != SqlTypes.AS) {
                result.addElement(LookupElementBuilder.create("AS "))
            }
        }

        for (item in SqlTokensSets.KEYWORDS.types) {
            result.addElement(
                LookupElementBuilder.create(item.toString())
                    .withCaseSensitivity(false)
                    .withInsertHandler(SqlCompletionInsertHandler)
            )
        }
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        if (context.completionType != CompletionType.SMART) {
            val file = context.file
            if (file is SqlExplytPsiFile) {
                val offset = context.startOffset
                val element = file.findElementAt(offset)
                if (element != null) {
                    if (element.parent is SqlIdentifier) {
                        context.dummyIdentifier = ""
                    }
                } else if (offset > 1 && file.findElementAt(offset - 1)?.text == "?") {
                    // for numeric input parameter completion
                    context.dummyIdentifier = "1"
                }
            }
        }
    }
}

private object SqlCompletionInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        if (item.lookupString.endsWith(')')) {
            val caretModel = context.editor.caretModel
            caretModel.moveToOffset(caretModel.offset - 1)
        }
    }

}