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

package com.explyt.spring.core.properties.providers

import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.properties.references.ExplytPropertyReference
import com.explyt.spring.core.properties.references.ValueHintReference
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.PropertyUtil.propertyKey
import com.explyt.spring.core.util.PropertyUtil.propertyValuePsiElement
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.intellij.util.text.findTextRange

class SpringConfigurationPropertiesValueReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (!SpringCoreUtil.isConfigurationPropertyFile(element.containingFile)) {
            return emptyArray()
        }
        val placeholderReferences = getPlaceholderValueReferences(element)
        if (placeholderReferences.isNotEmpty()) {
            return placeholderReferences.toTypedArray()
        }
        if (element.propertyKey() == SpringProperties.SPRING_PROFILES_ACTIVE) {
            return PsiReference.EMPTY_ARRAY
        }

        val valueElement = element.propertyValuePsiElement() ?: return emptyArray()
        val textRange = element.text.findTextRange(valueElement.text) ?: return emptyArray()
        return arrayOf(ValueHintReference(element, textRange))
    }

    private fun getPlaceholderValueReferences(element: PsiElement): List<PsiReference> {
        val text = element.text ?: return emptyList()

        return PropertyUtil.getPlaceholders(text) { placeholder, range ->
            ExplytPropertyReference(element, placeholder, range, true)
        }
    }
}