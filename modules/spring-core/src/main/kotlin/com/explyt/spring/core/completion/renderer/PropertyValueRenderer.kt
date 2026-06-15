/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.renderer

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.completion.properties.ValueHint
import com.explyt.util.ExplytTextUtil.getFirstSentenceWithoutDot
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer

class PropertyValueRenderer : LookupElementRenderer<LookupElement>() {

    override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
        val valueHint = element.`object` as ValueHint
        val lookupString = element.lookupString
        presentation.itemText = lookupString

        valueHint.description?.let {
            presentation.setTailText(" (" + getFirstSentenceWithoutDot(it) + ")", true)
        }

        presentation.icon = SpringIcons.Spring
    }

}