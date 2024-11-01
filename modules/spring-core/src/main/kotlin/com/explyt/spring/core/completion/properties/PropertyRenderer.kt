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

package com.explyt.spring.core.completion.properties

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