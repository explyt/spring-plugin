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