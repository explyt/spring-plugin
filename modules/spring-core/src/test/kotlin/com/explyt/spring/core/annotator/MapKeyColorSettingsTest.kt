/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.annotator

import com.explyt.spring.core.color.ExplytConfigurationHighlighting
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font

class MapKeyColorSettingsTest : ExplytJavaLightTestCase() {
    fun testMapKeyInitializationPreservesConfiguredColor() {
        val key = TextAttributesKey.createTextAttributesKey("EXPLYT_MAP_KEY_HIGHLIGHTER")
        val scheme = EditorColorsManager.getInstance().globalScheme
        val previous = scheme.getAttributes(key)?.clone()
        val custom = TextAttributes(Color.RED, null, null, null, Font.BOLD)
        try {
            scheme.setAttributes(key, custom)
            assertEquals(custom, scheme.getAttributes(ExplytConfigurationHighlighting.MAP_KEY_HIGHLIGHTER_KEY))
        } finally {
            scheme.setAttributes(key, previous)
        }
    }
}
