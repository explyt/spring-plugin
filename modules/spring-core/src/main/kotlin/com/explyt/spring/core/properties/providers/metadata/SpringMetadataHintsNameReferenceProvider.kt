/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.providers.metadata

import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.SpringProperties.POSTFIX_KEYS
import com.explyt.spring.core.SpringProperties.POSTFIX_VALUES
import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext


class SpringMetadataHintsNameReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val property = ElementManipulators.getValueText(element)
        val prefixLength = getPrefixLength(property)
        val textRange = if (prefixLength != -1) TextRange.from(1, prefixLength) else null
        val propertyKey = if (prefixLength != -1) property.substring(0, prefixLength) else property
        return arrayOf(
            ConfigurationPropertyKeyReference(
                element,
                module,
                propertyKey,
                textRange,
                SpringProperties.HINTS
            )
        )
    }

    private fun getPrefixLength(property: String): Int {
        var prefixLength = -1
        if (property.endsWith(POSTFIX_KEYS) || property.endsWith(POSTFIX_VALUES)) {
            prefixLength = property.lastIndexOf(POSTFIX_KEYS)
            if (prefixLength == -1) {
                prefixLength = property.lastIndexOf(POSTFIX_VALUES)
            }
        }
        return prefixLength
    }
}