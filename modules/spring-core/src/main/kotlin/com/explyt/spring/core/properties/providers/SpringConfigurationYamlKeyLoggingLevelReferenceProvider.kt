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

import com.explyt.spring.core.SpringProperties.LOGGING_LEVEL
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar

class SpringConfigurationYamlKeyLoggingLevelReferenceProvider : PsiReferenceProvider() {

    private val referenceProvider = JavaClassReferenceProvider()
        .apply { isSoft = true }

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val yamlScalar = element as? YAMLScalar ?: return PsiReference.EMPTY_ARRAY

        val yamlKeyValue = when (val yamlParent = yamlScalar.parent) {
            is YAMLMapping -> yamlParent.parent as? YAMLKeyValue ?: return PsiReference.EMPTY_ARRAY
            is YAMLKeyValue -> if (yamlParent.key?.text == "level") yamlParent else return PsiReference.EMPTY_ARRAY
            else -> return PsiReference.EMPTY_ARRAY
        }

        val fullKey = YAMLUtil.getConfigFullName(yamlKeyValue)

        if (fullKey != LOGGING_LEVEL) return PsiReference.EMPTY_ARRAY

        val textValue = yamlScalar.textValue
        val offset = ElementManipulators.getOffsetInElement(yamlScalar)

        return JavaClassReferenceSet(textValue, yamlScalar, offset, false, referenceProvider)
            .references
    }
}