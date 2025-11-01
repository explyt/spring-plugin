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