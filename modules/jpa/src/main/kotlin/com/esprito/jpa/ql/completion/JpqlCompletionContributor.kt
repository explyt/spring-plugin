package com.esprito.jpa.ql.completion

import com.esprito.jpa.ql.psi.*
import com.esprito.jpa.ql.psi.impl.JpqlElementFactory
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
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

            val elementName = (aliasDeclaration.referencedElement as? JpqlIdentifier?)?.name

            if (elementName != null) {
                linkedSetOf(
                    elementName.substring(0, 1).lowercase(),
                    convertToAbbreviation(elementName),
                    elementName.replaceFirstChar { it.lowercase() },
                ).forEach {
                    result.addElement(LookupElementBuilder.create(it))
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
                        return super.convertItem(item)
                    }

                    if (item in listOf(JpqlTypes.STRING, JpqlTypes.NUMERIC, JpqlTypes.DATETIME, JpqlTypes.ID)) {
                        return null
                    }

                    return item.toString()
                }
            }
        file.putUserData(GeneratedParserUtilBase.COMPLETION_STATE_KEY, state)
        TreeUtil.ensureParsed(file.node)

        for (item in state.items) {
            result.addElement(LookupElementBuilder.create(item))
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
}