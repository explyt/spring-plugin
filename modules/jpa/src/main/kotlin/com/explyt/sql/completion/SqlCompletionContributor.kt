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