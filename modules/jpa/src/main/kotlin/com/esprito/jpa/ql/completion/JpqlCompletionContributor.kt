package com.esprito.jpa.ql.completion

import com.esprito.jpa.ql.JpqlLanguage
import com.esprito.jpa.ql.psi.JpqlFile
import com.esprito.jpa.ql.psi.JpqlTypes
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType

class JpqlCompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.position.prevSibling.elementType == JpqlTypes.DOT) {
            return
        }

        val jpqlFile = parameters.position.containingFile as? JpqlFile ?: return

        val fragment = jpqlFile.text.substring(0, parameters.offset)
        val empty = StringUtil.isEmptyOrSpaces(fragment)
        val text = if (empty) "empty " else fragment
        val completionOffset = if (empty) 0 else fragment.length
        val file =
            PsiFileFactory.getInstance(jpqlFile.project)
                .createFileFromText("completion.jpql", JpqlLanguage.INSTANCE, text, true, false)
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
}