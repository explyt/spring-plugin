/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.sql.highlight

import com.explyt.sql.SqlExplytLanguage
import com.explyt.sql.psi.SqlLexerAdapter
import com.explyt.sql.psi.SqlTokensSets
import com.explyt.sql.psi.SqlTypes
import com.intellij.codeInsight.highlighting.HighlightErrorFilter
import com.intellij.lexer.Lexer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.tree.IElementType

class SqlSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer {
        return SqlLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        if (tokenType in SqlTokensSets.SEPARATORS) {
            return SEPARATOR_KEYS
        }

        if (tokenType in SqlTokensSets.COMMENTS) {
            return COMMENT_KEYS
        }

        if (tokenType in SqlTokensSets.KEYWORDS) {
            return KEY_KEYS
        }

        if (tokenType in SqlTokensSets.IDENTIFIERS) {
            return EMPTY_KEYS
        }

        if (tokenType in SqlTokensSets.STRING_LITERALS) {
            return STRING_LITERAL_KEYS
        }

        if (tokenType in SqlTokensSets.NUMERIC_LITERALS) {
            return NUMERIC_LITERAL_KEYS
        }

        if (tokenType in SqlTokensSets.DATETIME_LITERALS) {
            return DATETIME_LITERAL_KEYS
        }

        if (tokenType == SqlTypes.BOOLEAN) {
            return BOOLEAN_LITERAL_KEYS
        }

        return EMPTY_KEYS
    }

    companion object {
        private val SEPARATOR = createTextAttributesKey(
            "SQL_EXPLYT_SEPARATOR",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )
        private val KEY = createTextAttributesKey(
            "SQL_EXPLYT_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD
        )

        private val COMMENT = createTextAttributesKey(
            "SQL_EXPLYT_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )

        private val BAD_CHARACTER = createTextAttributesKey(
            "SQL_EXPLYT_BAD_CHARACTER",
            HighlighterColors.BAD_CHARACTER
        )

        private val STRING_LITERAL = createTextAttributesKey(
            "SQL_EXPLYT_STRING",
            DefaultLanguageHighlighterColors.STRING
        )

        private val NUMERIC_LITERAL = createTextAttributesKey(
            "SQL_EXPLYT_NUMERIC",
            DefaultLanguageHighlighterColors.NUMBER
        )

        private val DATETIME_LITERAL = createTextAttributesKey(
            "SQL_EXPLYT_DATETIME",
            DefaultLanguageHighlighterColors.NUMBER
        )

        private val SEPARATOR_KEYS = arrayOf(SEPARATOR)
        private val KEY_KEYS = arrayOf(KEY)
        private val STRING_LITERAL_KEYS = arrayOf(STRING_LITERAL)
        private val NUMERIC_LITERAL_KEYS = arrayOf(NUMERIC_LITERAL)
        private val BOOLEAN_LITERAL_KEYS = arrayOf(NUMERIC_LITERAL)
        private val DATETIME_LITERAL_KEYS = arrayOf(DATETIME_LITERAL)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val EMPTY_KEYS = arrayOf<TextAttributesKey>()
    }
}

class SQLExplytErrorFilter : HighlightErrorFilter() {
    override fun shouldHighlightErrorElement(element: PsiErrorElement): Boolean {
        if (ApplicationManager.getApplication().isUnitTestMode) return true
        return element.language != SqlExplytLanguage.INSTANCE
    }
}