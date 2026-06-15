/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.PATHS
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext

class SpringOpenApiJsonUrlEndpointReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(psiElement: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val nameElement = psiElement as? JsonStringLiteral ?: return emptyArray()

        val jsonProperty = nameElement.parentOfType<JsonProperty>() ?: return emptyArray()
        if (jsonProperty.nameElement != nameElement) return emptyArray()

        val parentProperty = jsonProperty.parentOfType<JsonProperty>() ?: return emptyArray()
        if (parentProperty.name == PATHS) {
            return getReferenceForUrl(nameElement)
        }

        return getReferenceForRequestMethod(nameElement, parentProperty)
    }

    private fun getReferenceForUrl(nameElement: JsonStringLiteral): Array<PsiReference> {
        val key = nameElement.value

        return arrayOf(
            ExplytControllerMethodReference(
                nameElement,
                key,
                null,
                ElementManipulators.getValueTextRange(nameElement),
                true
            )
        )
    }

    private fun getReferenceForRequestMethod(
        nameElement: JsonStringLiteral,
        urlProperty: JsonProperty
    ): Array<PsiReference> {
        val url = urlProperty.name
        val key = nameElement.value

        if (key !in SpringWebUtil.REQUEST_METHODS) return emptyArray()
        val pathElement = urlProperty.parentOfType<JsonProperty>() ?: return emptyArray()
        if (pathElement.name != PATHS) return emptyArray()

        return arrayOf(
            ExplytControllerMethodReference(
                nameElement,
                url,
                key.uppercase(),
                ElementManipulators.getValueTextRange(nameElement),
                true
            )
        )
    }

}