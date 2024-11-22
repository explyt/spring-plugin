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