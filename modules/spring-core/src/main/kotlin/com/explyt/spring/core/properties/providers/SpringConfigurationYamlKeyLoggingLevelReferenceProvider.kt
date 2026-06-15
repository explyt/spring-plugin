/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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