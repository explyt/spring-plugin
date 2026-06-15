/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.providers.metadata

import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet
import com.intellij.util.ProcessingContext

class SpringMetadataTypeReferenceProvider : PsiReferenceProvider() {

    private val referenceProvider = JavaClassReferenceProvider()

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val valueText = ElementManipulators.getValueText(element)
        val offset = ElementManipulators.getOffsetInElement(element)

        return JavaClassReferenceSet(valueText, element, offset, false, referenceProvider)
            .references
    }

}