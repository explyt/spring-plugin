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

package com.explyt.spring.web.language.http

import com.explyt.spring.web.language.http.psi.HttpTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class HttpSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer {
        return HttpLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            HttpTypes.REQUEST_SEPARATOR    -> REQUEST_SEPARATOR_KEYS
            in COMMENT_TYPES               -> COMMENT_KEYS
            HttpTypes.META_TOKEN           -> META_TOKEN_KEYS
            HttpTypes.TAG_TOKEN            -> TAG_TOKEN_KEYS
            HttpTypes.IDENTIFIER           -> IDENTIFIER_KEYS
            HttpTypes.HTTP_TOKEN           -> HTTP_TOKEN_KEYS
            HttpTypes.REQUEST_TARGET_VALUE -> REQUEST_TARGET_KEYS
            HttpTypes.HTTP_VERSION         -> HTTP_VERSION_KEYS
            HttpTypes.FIELD_CONTENT_TOKEN  -> FIELD_CONTENT_KEYS
            HttpTypes.REQUEST_BODY_VALUE   -> REQUEST_BODY_KEYS
            TokenType.BAD_CHARACTER        -> BAD_CHARACTER_KEYS
            else                           -> EMPTY_KEYS
        }
    }

    companion object {
        private val COMMENT_TYPES = setOf(HttpTypes.COMMENT_LINE, HttpTypes.COMMENT_SEPARATOR)

        @JvmStatic
        val REQUEST_SEPARATOR = createTextAttributesKey(
            "HTTP_REQUEST_SEPARATOR",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )

        @JvmStatic
        val COMMENT = createTextAttributesKey(
            "HTTP_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )

        @JvmStatic
        val META_TOKEN = createTextAttributesKey(
            "HTTP_META_TOKEN",
            DefaultLanguageHighlighterColors.IDENTIFIER
        )

        @JvmStatic
        val TAG_TOKEN = createTextAttributesKey(
            "HTTP_TAG_TOKEN",
            DefaultLanguageHighlighterColors.METADATA
        )

        @JvmStatic
        val IDENTIFIER = createTextAttributesKey(
            "HTTP_VARIABLE_IDENTIFIER",
            DefaultLanguageHighlighterColors.CONSTANT
        )

        @JvmStatic
        val HTTP_TOKEN = createTextAttributesKey(
            "HTTP_TOKEN",
            DefaultLanguageHighlighterColors.KEYWORD
        )

        @JvmStatic
        val REQUEST_TARGET = createTextAttributesKey(
            "HTTP_REQUEST_TARGET",
            DefaultLanguageHighlighterColors.HIGHLIGHTED_REFERENCE
        )

        @JvmStatic
        val HTTP_VERSION = createTextAttributesKey(
            "HTTP_VERSION",
            DefaultLanguageHighlighterColors.NUMBER
        )

        @JvmStatic
        val FIELD_CONTENT = createTextAttributesKey(
            "HTTP_FIELD_CONTENT",
            DefaultLanguageHighlighterColors.STRING
        )

        @JvmStatic
        val REQUEST_BODY = createTextAttributesKey(
            "HTTP_REQUEST_BODY",
            DefaultLanguageHighlighterColors.STRING
        )

        @JvmStatic
        val REQUEST_SEPARATOR_KEYS = arrayOf(REQUEST_SEPARATOR)

        @JvmStatic
        val COMMENT_KEYS = arrayOf(COMMENT)

        @JvmStatic
        val META_TOKEN_KEYS = arrayOf(META_TOKEN)

        @JvmStatic
        val TAG_TOKEN_KEYS = arrayOf(TAG_TOKEN)

        @JvmStatic
        val IDENTIFIER_KEYS = arrayOf(IDENTIFIER)

        @JvmStatic
        val HTTP_TOKEN_KEYS = arrayOf(HTTP_TOKEN)

        @JvmStatic
        val REQUEST_TARGET_KEYS = arrayOf(REQUEST_TARGET)

        @JvmStatic
        val HTTP_VERSION_KEYS = arrayOf(HTTP_VERSION)

        @JvmStatic
        val FIELD_CONTENT_KEYS = arrayOf(FIELD_CONTENT)

        @JvmStatic
        val REQUEST_BODY_KEYS = arrayOf(REQUEST_BODY)

        @JvmStatic
        val BAD_CHARACTER_KEYS = arrayOf(createTextAttributesKey(
            "HTTP_BAD_CHARACTER",
            HighlighterColors.BAD_CHARACTER
        ))

        @JvmStatic
        val EMPTY_KEYS = arrayOf<TextAttributesKey>()
    }

}