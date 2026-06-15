/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
            # Comment.
            # @tag
            POST /path HTTP/1.0
            Content-Type: text\/html; charset=utf-8
            Content-Length: 12

            Hello world!
            
            ### Request 2
            // Another comment.
            // @tag with meta-information
            GET /path
            
            {{variable}}
        """.trimIndent()
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, TextAttributesKey>? {
        return null
    }

    companion object {
        @JvmStatic
        val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Pre-request comments//Request separator",
                HttpSyntaxHighlighter.REQUEST_SEPARATOR),
            AttributesDescriptor("Pre-request comments//Comment", HttpSyntaxHighlighter.COMMENT),
            AttributesDescriptor("Pre-request comments//Meta token", HttpSyntaxHighlighter.META_TOKEN),
            AttributesDescriptor("Pre-request comments//Tag token", HttpSyntaxHighlighter.TAG_TOKEN),

            AttributesDescriptor("Request//HTTP token", HttpSyntaxHighlighter.HTTP_TOKEN),
            AttributesDescriptor("Request//Request target", HttpSyntaxHighlighter.REQUEST_TARGET),
            AttributesDescriptor("Request//HTTP version", HttpSyntaxHighlighter.HTTP_VERSION),
            AttributesDescriptor("Request//Field content", HttpSyntaxHighlighter.FIELD_CONTENT),
            AttributesDescriptor("Request//Message body", HttpSyntaxHighlighter.REQUEST_BODY),

            AttributesDescriptor("Variable identifier", HttpSyntaxHighlighter.IDENTIFIER),
        )
    }

}