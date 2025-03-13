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

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class HttpColorSettingsPage : ColorSettingsPage {
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        return DESCRIPTORS
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> {
        return ColorDescriptor.EMPTY_ARRAY
    }

    override fun getDisplayName(): String {
        return "HTTP"
    }

    override fun getIcon(): Icon {
        return HttpFileType.INSTANCE.getIcon()
    }

    override fun getHighlighter(): SyntaxHighlighter {
        return HttpSyntaxHighlighter()
    }

    override fun getDemoText(): String {
        return """
            ### Request 1
            # Some comment.
            # @tag with values
            POST /path HTTP/1.0
            Content-Type: text/html; charset=utf-8
            Content-Length: 12

            Hello world!
            
            ### Request 2
            // Another comment!
            GET /path
        """.trimIndent()
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, TextAttributesKey>? {
        return null
    }

    companion object {
        @JvmStatic
        val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Pre-request comments//Request definer",
                HttpSyntaxHighlighterAnnotator.REQUEST_DEFINER),
            AttributesDescriptor("Pre-request comments//Comment", HttpSyntaxHighlighter.COMMENT),
            AttributesDescriptor("Pre-request comments//Tag token", HttpSyntaxHighlighter.TAG_TOKEN),

            AttributesDescriptor("Request//HTTP token", HttpSyntaxHighlighter.HTTP_TOKEN),
            AttributesDescriptor("Request//Request target", HttpSyntaxHighlighter.REQUEST_TARGET),
            AttributesDescriptor("Request//HTTP version", HttpSyntaxHighlighter.HTTP_VERSION),
            AttributesDescriptor("Request//Field content", HttpSyntaxHighlighter.FIELD_CONTENT),
            AttributesDescriptor("Request//Message body", HttpSyntaxHighlighter.REQUEST_BODY),
        )
    }

}