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

package com.explyt.spring.core.completion.yaml

import com.explyt.spring.core.SpringProperties.COLON
import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.EditorModificationUtilEx
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.parentOfType
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
            if (!deleteCurrentElement(context, currentElement)) return

            val parentMapping = foundParentKey.value as? YAMLMapping
            if (parentMapping != null) {
                val newName = configurationProperty.name.substringAfter("$parentConfigFullName.")
                buildAndInsertKeyValue(context, foundParentKey, newName, configurationProperty.isMap())
                StatisticService.getInstance().addActionUsage(StatisticActionId.COMPLETION_YAML_KEY_CONFIGURATION)
            }
        } else {
            insertNewKeyValue(lookupElement, context, configurationProperty, currentElement)
            StatisticService.getInstance().addActionUsage(StatisticActionId.COMPLETION_YAML_KEY_CONFIGURATION)
        }
    }

    private fun insertNewKeyValue(
        lookupElement: LookupElement,
        context: InsertionContext,
        configurationProperty: ConfigurationProperty,
        currentElement: PsiElement
    ) {
        val lookupString = lookupElement.lookupString
        val existingIndentation = getExistingIndentation(context, lookupString)
        val indentPerLevel = getCodeStyleIntent(context)
        val suggestionWithCaret =
            getSuggestionReplacementWithCaret(lookupString, existingIndentation, indentPerLevel, configurationProperty)
        val suggestionWithoutCaret = suggestionWithCaret.replace(CARET_MARKUP, StringUtils.EMPTY)

        if (!deleteCurrentElement(context, currentElement)) return

        EditorModificationUtilEx.insertStringAtCaret(
            context.editor, suggestionWithoutCaret, false, true,
            getCaretIndex(suggestionWithCaret)
        )
        if (configurationProperty.isMap()) {
            val indent = CodeStyle.getIndentSize(context.file)
            val tabString = StringUtil.repeatSymbol(' ', indent * (lookupString.split('.').lastIndex + 1))
            EditorModificationUtilEx.insertStringAtCaret(context.editor, "\n$tabString")
        }
        val project = context.editor.project ?: return
        AutoPopupController.getInstance(project).scheduleAutoPopup(context.editor)
    }

    private fun deleteCurrentElement(
        context: InsertionContext, currentElement: PsiElement
    ): Boolean {
        deleteLookupTextAndRetrieveOldValue(context, currentElement)
        return true
    }

    private fun buildAndInsertKeyValue(
        context: InsertionContext,
        foundParentKey: YAMLKeyValue,
        key: String,
        isAddLastLine: Boolean
    ) {
        if (key.isEmpty()) {
            return
        }
        val mapping = foundParentKey.value as? YAMLMapping ?: return

        val indent = CodeStyle.getIndentSize(context.file)
        val identFountElement = YAMLUtil.getIndentToThisElement(foundParentKey)

        val keyValue = StringBuilder()
        val keys = key.split('.')
        for ((index, keyPart) in keys.withIndex()) {
            val tabString = StringUtil.repeatSymbol(' ', (indent * (index + 1)) + identFountElement)
            keyValue.append("$tabString$keyPart:")
            if (index == keys.size - 1) {
                keyValue.append(" ")
            } else {
                keyValue.append("\n")
            }
        }
        if (isAddLastLine) {
            val tabString = StringUtil.repeatSymbol(' ', (indent * (keys.lastIndex + 2)) + identFountElement)
            keyValue.append("\n$tabString")
        }

        val editor = context.editor
        editor.caretModel.moveToOffset(foundParentKey.endOffset)

        val keyElement = mapping.getKeyValueByKey(key)
        if (keyElement == null) {
            EditorModificationUtilEx.insertStringAtCaret(context.editor, "\n")
            EditorModificationUtilEx.insertStringAtCaret(context.editor, keyValue.toString())
            val column = foundParentKey.endOffset + indent * keys.lastIndex
            editor.caretModel.moveCaretRelatively(column, 0, false, false, true)
        } else {
            editor.caretModel.moveToOffset(keyElement.endOffset)
        }

        val project = editor.project ?: return
        AutoPopupController.getInstance(project).scheduleAutoPopup(context.editor)

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
        indentPerLevel: String,
        configurationProperty: ConfigurationProperty,
    ): String {
        val builder = getSuggestStringBuilder(suggestion, existingIndentation, indentPerLevel, configurationProperty)
        val suffix = StringUtils.SPACE + CARET_MARKUP
        builder.append(suffix)
        return builder.toString()
    }

    private fun getSuggestStringBuilder(
        suggestion: String,
        existingIndentation: String,
        indentPerLevel: String,
        configurationProperty: ConfigurationProperty
    ): StringBuilder {
        if (configurationProperty.inLineYaml) {
            return StringBuilder(suggestion).append(COLON)
        }
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
        return builder.delete(0, existingIndentation.length + 1)
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