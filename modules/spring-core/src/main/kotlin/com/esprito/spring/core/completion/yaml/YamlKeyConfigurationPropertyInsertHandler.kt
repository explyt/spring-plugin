package com.esprito.spring.core.completion.yaml

import com.esprito.spring.core.SpringProperties.COLON
import com.esprito.spring.core.completion.properties.ConfigurationProperty
import com.esprito.spring.core.util.YamlUtil
import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.EditorModificationUtilEx
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.endOffset
import org.apache.commons.lang3.StringUtils
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLPsiElement
import java.util.concurrent.atomic.AtomicInteger

class YamlKeyConfigurationPropertyInsertHandler : InsertHandler<LookupElement> {

    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        if (nextCharAfterSpacesAndQuotesIsColon(getStringAfterAutoCompletedValue(context))) {
            return
        }

        val configurationProperty = lookupElement.`object` as? ConfigurationProperty ?: return
        val propertyNames = configurationProperty.name.split('.')
        val currentElement = context.file.findElementAt(context.startOffset) ?: return
        val yamlDocument = currentElement.parentOfType<YAMLDocument>() ?: return
        val currentPsiElement = currentElement.parentOfType<YAMLPsiElement>()
        val currentConfigParts = if (currentPsiElement != null) {
            YAMLUtil.getConfigFullName(currentPsiElement)
        } else {
            ""
        }

        val foundParentKey = (propertyNames.size - 1 downTo 1).firstNotNullOfOrNull {
            val parentKeys = propertyNames.subList(0, it)
            val qualifiedKeyInDocument = YAMLUtil.getQualifiedKeyInDocument(yamlDocument, parentKeys)
            qualifiedKeyInDocument
        }

        val parentConfigFullName = if (foundParentKey == null) "" else YAMLUtil.getConfigFullName(foundParentKey)
        if (foundParentKey != null && currentConfigParts != parentConfigFullName) {
            if (!deleteCurrentElement(context)) return

            val parentMapping = foundParentKey.value as? YAMLMapping
            if (parentMapping != null) {
                val newName = configurationProperty.name.substringAfter("$parentConfigFullName.")
                createAndAddKey(context, newName, parentMapping)
                context.editor.caretModel.moveToOffset(foundParentKey.endOffset)
            }
        } else {
            val lookupString = lookupElement.lookupString
            val existingIndentation = getExistingIndentation(context, lookupString)
            val indentPerLevel = getCodeStyleIntent(context)
            val suggestionWithCaret =
                getSuggestionReplacementWithCaret(lookupString, existingIndentation, indentPerLevel)
            val suggestionWithoutCaret = suggestionWithCaret.replace(CARET_MARKUP, StringUtils.EMPTY)

            if (!deleteCurrentElement(context)) return

            EditorModificationUtilEx.insertStringAtCaret(
                context.editor, suggestionWithoutCaret, false, true,
                getCaretIndex(suggestionWithCaret)
            )
        }
    }

    private fun deleteCurrentElement(
        context: InsertionContext
    ): Boolean {
        val currentElement = context.file.findElementAt(context.startOffset) ?: return false
        deleteLookupTextAndRetrieveOldValue(context, currentElement)
        return true
    }

    private fun createAndAddKey(
        context: InsertionContext,
        key: String,
        mapping: YAMLMapping
    ) {
        if (key.isEmpty()) {
            return
        }
        val keyValue = StringBuilder()
        val keys = key.split('.')
        for ((index, keyPart) in keys.withIndex()) {
            keyValue.append("$keyPart:")
            if (index == keys.size - 1) {
                keyValue.append(" ")
            } else {
                val tabString = StringUtil.repeatSymbol(' ', CodeStyle.getIndentSize(context.file) * (index + 1))
                keyValue.append("\n$tabString")
            }
        }

        val dummyYamlMapping = YamlUtil.createYamlMapping(mapping.project, keyValue.toString())
        val createdKey = dummyYamlMapping.keyValues.first()
        mapping.putKeyValue(createdKey)
    }

    private fun getCaretIndex(suggestionWithCaret: String): Int {
        return suggestionWithCaret.indexOf(CARET_MARKUP)
    }

    private fun getExistingIndentation(context: InsertionContext, lookupString: String): String {
        val stringBeforeAutoCompletedValue = getStringBeforeAutoCompletedValue(context, lookupString)
        return getExistingIndentationInRowStartingFromEnd(stringBeforeAutoCompletedValue)
    }

    private fun getStringAfterAutoCompletedValue(context: InsertionContext): String {
        return context.document.text.substring(context.tailOffset)
    }

    private fun getStringBeforeAutoCompletedValue(context: InsertionContext, lookupString: String): String {
        return context.document.text
            .substring(0, context.tailOffset - lookupString.length)
    }

    private fun nextCharAfterSpacesAndQuotesIsColon(string: String): Boolean {
        for (element in string) {
            if (element != ' ' && element != '"') {
                return element == ':'
            }
        }
        return false
    }

    private fun getExistingIndentationInRowStartingFromEnd(value: String): String {
        var count = 0
        for (i in value.length - 1 downTo 0) {
            val c = value[i]
            if (c != '\t' && c != ' ' && c != '-') {
                break
            }
            count++
        }
        return value.substring(value.length - count).replace("-", StringUtils.SPACE)
    }

    private fun deleteLookupTextAndRetrieveOldValue(context: InsertionContext, elementAtCaret: PsiElement) {
        if (elementAtCaret.node.elementType !== YAMLTokenTypes.SCALAR_KEY) {
            deleteLookupPlain(context)
        } else {
            val keyValue = PsiTreeUtil.getParentOfType(
                elementAtCaret,
                YAMLKeyValue::class.java
            )!!
            context.commitDocument()

            if (keyValue.value != null) {
                val dummyKV = YAMLElementGenerator.getInstance(context.project).createYamlKeyValue("foo", "b")
                dummyKV.setValue(keyValue.value!!)
            }
            context.tailOffset = keyValue.textRange.endOffset
            WriteCommandAction.runWriteCommandAction(
                context.project
            ) { keyValue.parentMapping!!.deleteKeyValue(keyValue) }
        }
    }

    private fun deleteLookupPlain(context: InsertionContext) {
        val document = context.document
        document.deleteString(context.startOffset, context.tailOffset)
        context.commitDocument()
    }

    private fun getSuggestionReplacementWithCaret(
        suggestion: String,
        existingIndentation: String,
        indentPerLevel: String
    ): String {
        val matchesTopFirst = suggestion.split('.').dropLastWhile { it.isEmpty() }.toTypedArray()
        val builder = StringBuilder()
        val count = AtomicInteger(0)
        for (word in matchesTopFirst) {
            builder.append(StringUtils.LF)
                .append(existingIndentation)
                .append(getIndent(indentPerLevel, count.getAndIncrement()))
                .append(word)
                .append(COLON)
        }
        builder.delete(0, existingIndentation.length + 1)
        val suffix = StringUtils.SPACE + CARET_MARKUP
        builder.append(suffix)
        return builder.toString()
    }

    companion object {

        const val CARET_MARKUP = "<caret>"

        fun getCodeStyleIntent(insertionContext: InsertionContext): String {
            return StringUtil.repeatSymbol(' ', CodeStyle.getIndentSize(insertionContext.file))
        }

        fun getIndent(indent: String, numOfHops: Int): String {
            return if (numOfHops == 0) StringUtils.EMPTY else indent.repeat(0.coerceAtLeast(numOfHops))
        }
    }
}