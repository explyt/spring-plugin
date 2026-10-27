/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.providers

import com.explyt.spring.core.SpringProperties.LOGGING_LEVEL
import com.explyt.spring.core.SpringProperties.PLACEHOLDER_PREFIX
import com.explyt.spring.core.properties.references.LoggingLevelKeys
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

class SpringConfigurationPropertiesKeyLoggingLevelReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val valueTextRange = ElementManipulators.getValueTextRange(element)
        val startInElement = LOGGING_LEVEL.length + 1

        if (startInElement > valueTextRange.length) return PsiReference.EMPTY_ARRAY

        val textRange = valueTextRange.shiftRight(startInElement).grown(-startInElement)

        val rangeText = textRange.substring(element.text)
        if (rangeText.contains(PLACEHOLDER_PREFIX)) return PsiReference.EMPTY_ARRAY

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return PsiReference.EMPTY_ARRAY
        return LoggingLevelKeys.referencesForSuffix(element, module, rangeText, textRange, offerGroupVariants = true)
    }
}
