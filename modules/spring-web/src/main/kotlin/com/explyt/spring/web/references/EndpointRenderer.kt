/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.references

import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointVerbs
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer

class EndpointRenderer : LookupElementRenderer<LookupElement>() {
    override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
        val endpointElement = element.`object` as? EndpointElement ?: return

        presentation.itemText = endpointElement.path
        val containing = endpointElement.containingClass?.name ?: endpointElement.containingFile?.name ?: ""
        val requestMethodsView = EndpointVerbs.labelsOf(endpointElement)
            .joinToString(prefix = "[", separator = ", ", postfix = "]")

        presentation.typeText = "$requestMethodsView $containing"
    }
}