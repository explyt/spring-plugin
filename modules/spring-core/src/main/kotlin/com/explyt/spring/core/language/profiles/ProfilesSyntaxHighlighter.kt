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

package com.explyt.spring.core.language.profiles

import com.explyt.spring.core.language.profiles.psi.ProfilesTokenSets
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class ProfilesSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = ProfilesLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        val textAttributesKeys = when (tokenType) {
            in ProfilesTokenSets.SEPARATORS -> SEPARATOR_KEYS
            in ProfilesTokenSets.OPERATORS -> OPERATOR_KEYS
            in ProfilesTokenSets.IDENTIFIERS -> VALUE_KEYS
            TokenType.BAD_CHARACTER -> BAD_CHAR_KEYS
            else -> EMPTY_KEYS
        }
        return textAttributesKeys
    }

    companion object {
        private val SEPARATOR = TextAttributesKey.createTextAttributesKey(
            "PROFILE_SEPARATOR",
            DefaultLanguageHighlighterColors.PARENTHESES
        )
        private val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey(
            "PROFILE_BAD_CHARACTER",
            HighlighterColors.BAD_CHARACTER
        )
        private val VALUE = TextAttributesKey.createTextAttributesKey(
            "PROFILE_VALUE",
            DefaultLanguageHighlighterColors.IDENTIFIER
        )
        private val OPERATOR = TextAttributesKey.createTextAttributesKey(
            "PROFILE_OPERATION",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )

        private val BAD_CHAR_KEYS = arrayOf(BAD_CHARACTER)
        private val SEPARATOR_KEYS = arrayOf(SEPARATOR)
        private val OPERATOR_KEYS = arrayOf(OPERATOR)
        private val VALUE_KEYS = arrayOf(VALUE)
        private val EMPTY_KEYS = arrayOf<TextAttributesKey>()
    }

}