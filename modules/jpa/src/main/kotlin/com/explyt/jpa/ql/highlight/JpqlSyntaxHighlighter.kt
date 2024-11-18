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

package com.explyt.jpa.ql.highlight

import com.explyt.jpa.ql.psi.JpqlLexerAdapter
import com.explyt.jpa.ql.psi.JpqlTokensSets
import com.explyt.jpa.ql.psi.JpqlTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class JpqlSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer {
        return JpqlLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        if (tokenType in JpqlTokensSets.SEPARATORS) {
            return SEPARATOR_KEYS
        }

        if (tokenType in JpqlTokensSets.COMMENTS) {
            return COMMENT_KEYS
        }

        if (tokenType in JpqlTokensSets.KEYWORDS) {
            return KEY_KEYS
        }

        if (tokenType in JpqlTokensSets.IDENTIFIERS) {
            return EMPTY_KEYS
        }

        if (tokenType in JpqlTokensSets.STRING_LITERALS) {
            return STRING_LITERAL_KEYS
        }

        if (tokenType in JpqlTokensSets.NUMERIC_LITERALS) {
            return NUMERIC_LITERAL_KEYS
        }

        if (tokenType in JpqlTokensSets.DATETIME_LITERALS) {
            return DATETIME_LITERAL_KEYS
        }

        if (tokenType == JpqlTypes.BOOLEAN) {
            return BOOLEAN_LITERAL_KEYS
        }

        return EMPTY_KEYS
    }

    companion object {
        private val SEPARATOR = createTextAttributesKey(
            "JPQL_SEPARATOR",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )
        private val KEY = createTextAttributesKey(
            "JPQL_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD
        )

        private val COMMENT = createTextAttributesKey(
            "JPQL_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )

        private val BAD_CHARACTER = createTextAttributesKey(
            "JPQL_BAD_CHARACTER",
            HighlighterColors.BAD_CHARACTER
        )

        private val STRING_LITERAL = createTextAttributesKey(
            "JPQL_STRING",
            DefaultLanguageHighlighterColors.STRING
        )

        private val NUMERIC_LITERAL = createTextAttributesKey(
            "JPQL_NUMERIC",
            DefaultLanguageHighlighterColors.NUMBER
        )

        private val DATETIME_LITERAL = createTextAttributesKey(
            "JPQL_DATETIME",
            DefaultLanguageHighlighterColors.NUMBER
        )

        private val BAD_CHAR_KEYS = arrayOf(BAD_CHARACTER)
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