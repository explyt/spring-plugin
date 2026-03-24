/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.color

import com.intellij.icons.AllIcons
import com.intellij.lang.properties.PropertiesHighlighterImpl
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class ExplytPropertyColorSettingsPage : ColorSettingsPage {
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        return arrayOf(
            AttributesDescriptor("Profile", ExplytConfigurationHighlighting.PROFILE_HIGHLIGHTER_KEY),
            AttributesDescriptor("Bean reference", ExplytConfigurationHighlighting.BEAN_HIGHLIGHTER_KEY)
        )
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "Explyt Colors"

    override fun getIcon(): Icon = AllIcons.FileTypes.Properties

    override fun getHighlighter(): SyntaxHighlighter {
        return PropertiesHighlighterImpl()
    }

    override fun getDemoText(): String {
        return """
# Example of properties
spring.profiles.active=<explyt_profile>dev</explyt_profile>
main.bean-component=<explyt_bean_reference>fooBeanComponent</explyt_bean_reference>
            
# Example of yaml
spring:
  profiles:
    active: <explyt_profile>dev</explyt_profile>
main:
  bean-component: <explyt_bean_reference>fooBeanComponent</explyt_bean_reference>
        """.trimIndent()
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, TextAttributesKey> {
        return mutableMapOf(
            "explyt_profile" to ExplytConfigurationHighlighting.PROFILE_HIGHLIGHTER_KEY,
            "explyt_bean_reference" to ExplytConfigurationHighlighting.BEAN_HIGHLIGHTER_KEY
        )
    }
}