/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.color

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font

object ExplytConfigurationHighlighting {

    val BEAN_HIGHLIGHTER_KEY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "BEAN_HIGHLIGHTER",
        DefaultLanguageHighlighterColors.HIGHLIGHTED_REFERENCE
    )

    val PROFILE_HIGHLIGHTER_KEY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "PROFILE_HIGHLIGHTER",
        DefaultLanguageHighlighterColors.KEYWORD
    )

    init {
        val beanTextAttributes =
            TextAttributes(Color(0x63be78), null, Color(0x63be78), EffectType.LINE_UNDERSCORE, Font.ITALIC)
        val profileTextAttributes = TextAttributes(Color(0xFF9800), null, null, null, Font.ITALIC)

        val colorsManager = EditorColorsManager.getInstance()
        colorsManager.allSchemes.forEach {
            it.setAttributes(BEAN_HIGHLIGHTER_KEY, beanTextAttributes)
            it.setAttributes(PROFILE_HIGHLIGHTER_KEY, profileTextAttributes)
        }
    }
}