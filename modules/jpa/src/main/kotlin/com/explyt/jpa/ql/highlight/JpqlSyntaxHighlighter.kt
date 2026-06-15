/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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