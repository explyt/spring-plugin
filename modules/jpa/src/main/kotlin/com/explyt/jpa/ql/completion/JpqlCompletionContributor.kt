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

package com.explyt.jpa.ql.completion

import com.explyt.jpa.JpaIcons
import com.explyt.jpa.ql.psi.*
import com.explyt.jpa.ql.psi.impl.JpqlElementFactory
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.prevLeaf

class JpqlCompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position
        if (position.prevLeaf(skipEmptyElements = true).elementType == JpqlTypes.DOT) {
            return
        }

        val aliasDeclaration = position.parentOfType<JpqlAliasDeclaration>()
        if (aliasDeclaration != null) {
            if (aliasDeclaration.firstChild?.elementType != JpqlTypes.AS) {
                result.addElement(LookupElementBuilder.create("AS "))
            }

            val elementName = when (val referencedElement = aliasDeclaration.referencedElement) {
                is JpqlIdentifier -> referencedElement.name
                is JpqlPathReferenceExpression -> referencedElement.identifierList.lastOrNull()?.name
                else -> null
            }

            if (elementName != null) {
                linkedSetOf(
                    elementName.substring(0, 1).lowercase(),
                    convertToAbbreviation(elementName),
                    elementName.replaceFirstChar { it.lowercase() },
                ).forEach {
                    result.addElement(
                        LookupElementBuilder.create(it)
                            .withTypeText("new alias", true)
                            .withIcon(JpaIcons.Alias)
                    )
                }
            }
        }

        if (position.parentOfType<JpqlEntityAccess>() != null) {
            return
        }

        val jpqlFile = position.containingFile as? JpqlFile ?: return

        val fragment = jpqlFile.text.substring(0, parameters.offset)

        val empty = StringUtil.isEmptyOrSpaces(fragment)
        val text = if (empty) "" else fragment

        val completionOffset = if (empty) 0 else fragment.length
        val file = JpqlElementFactory.getInstance(jpqlFile.project)
            .createFile(text, eventSystemEnabled = true)

        val state: GeneratedParserUtilBase.CompletionState =
            object : GeneratedParserUtilBase.CompletionState(completionOffset) {
                override fun convertItem(item: Any): String? {
                    if (item is Array<*> && item.isArrayOf<IElementType>()) {
                        if (item.lastOrNull() == JpqlTypes.LPAREN) {
                            return listOf(*item, JpqlTypes.RPAREN).joinToString(separator = "")
                        }

                        return super.convertItem(item)
                    }

                    if (item in LITERAL_TERMINALS) {
                        return null
                    }

                    return item.toString()
                }
            }
        file.putUserData(GeneratedParserUtilBase.COMPLETION_STATE_KEY, state)
        TreeUtil.ensureParsed(file.node)

        for (item in state.items) {
            result.addElement(
                LookupElementBuilder.create(item)
                    .withCaseSensitivity(false)
                    .withInsertHandler(JpqlCompletionInsertHandler)
            )
        }
    }

    private fun convertToAbbreviation(input: String): String {
        val words = input.split(Regex("(?=[A-Z])"))
            .filter { it.isNotEmpty() }

        val abbreviation = StringBuilder()
        for (word in words) {
            abbreviation.append(word[0].lowercase())
        }
        return abbreviation.toString()
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        if (context.completionType != CompletionType.SMART) {
            val file = context.file
            if (file is JpqlFile) {
                val offset = context.startOffset
                val element = file.findElementAt(offset)
                if (element != null) {
                    if (element.parent is JpqlIdentifier) {
                        context.dummyIdentifier = ""
                    }
                } else if (offset > 1 && file.findElementAt(offset - 1)?.text == "?") {
                    // for numeric input parameter completion
                    context.dummyIdentifier = "1"
                }
            }
        }
    }

    companion object {
        private val LITERAL_TERMINALS = listOf(
            JpqlTypes.STRING,
            JpqlTypes.NUMERIC,
            JpqlTypes.DATETIME,
            JpqlTypes.ID,
            JpqlTypes.NAMED_INPUT_PARAMETER,
            JpqlTypes.NUMERIC_INPUT_PARAMETER
        )
    }
}

private object JpqlCompletionInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        if (item.lookupString.endsWith(')')) {
            val caretModel = context.editor.caretModel
            caretModel.moveToOffset(caretModel.offset - 1)
        }
    }

}