package com.esprito.spring.web.references

import com.esprito.spring.web.service.beans.discoverer.EndpointElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer

class EndpointRenderer : LookupElementRenderer<LookupElement>() {
    override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
        val endpointElement = element.`object` as? EndpointElement ?: return

        presentation.itemText = endpointElement.path
        val containing = endpointElement.containingClass?.name ?: endpointElement.containingFile?.name ?: ""
        val requestMethodsView = if (endpointElement.requestMethods.isEmpty()) {
            ""
        } else {
            endpointElement.requestMethods.joinToString(prefix = "[", separator = ", ", postfix = "]")
        }

        presentation.typeText = "$requestMethodsView $containing"
    }
}