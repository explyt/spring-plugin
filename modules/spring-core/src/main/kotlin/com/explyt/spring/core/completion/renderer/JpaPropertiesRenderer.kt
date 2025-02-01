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

package com.explyt.spring.core.completion.renderer

import com.explyt.spring.core.SpringProperties.SPRING_JPA_PROPERTIES
import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import javax.swing.Icon

class JpaRendererPropertyRenderer(
    private val typeItem: String,
    private val icon: Icon
) : LookupElementRenderer<LookupElement>() {
    override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
        val lookupString = element.lookupString
        presentation.itemText = lookupString.substringAfter("$SPRING_JPA_PROPERTIES.")
        presentation.typeText = typeItem
        presentation.icon = icon
        val configurationProperty = element.`object` as ConfigurationProperty
        if (configurationProperty.deprecation != null) {
            presentation.setStrikeout(true)
        }
    }
}