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