/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.renderer

import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.explyt.spring.core.completion.properties.DeprecationInfoLevel
import com.explyt.util.ExplytTextUtil.getFirstSentenceWithoutDot
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.JBColor
import org.apache.commons.lang3.StringUtils
import java.util.regex.Pattern

class PropertyRenderer : LookupElementRenderer<LookupElement>() {
    private val packageRemovalPattern = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*\\.")

    override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
        val configurationProperty = element.`object` as ConfigurationProperty
        val lookupString = element.lookupString
        presentation.itemText = lookupString
        val defaultValue: Any? = configurationProperty.defaultValue
        if (defaultValue != null) {
            val shortDescription = StringUtil.shortenTextWithEllipsis(defaultValue.toString(), 60, 0, true)
            presentation.setTailText("=$shortDescription", JBColor.GREEN)
        }

        if (configurationProperty.type != null) {
            presentation.typeText = shortenedType(configurationProperty.type)
        }

        configurationProperty.description?.let {
            presentation.appendTailText(" (" + getFirstSentenceWithoutDot(it) + ")", true)
        }
        val deprecation = configurationProperty.deprecation
        if (deprecation != null) {
            presentation.setStrikeout(true)
            if (deprecation.level === DeprecationInfoLevel.ERROR) {
                presentation.setItemTextForeground(JBColor.RED)
            }
        }

        presentation.icon = if (configurationProperty.isMap()) {
            AllIcons.Nodes.PropertyWrite
        } else {
            AllIcons.Nodes.Property
        }
    }

    private fun shortenedType(type: String?): String? {
        if (type == null) return null
        val matcher = packageRemovalPattern.matcher(type)
        return if (matcher.find()) matcher.replaceAll(StringUtils.EMPTY) else type
    }
}