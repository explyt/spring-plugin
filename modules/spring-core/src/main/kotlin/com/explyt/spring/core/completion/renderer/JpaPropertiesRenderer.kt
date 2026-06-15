/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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