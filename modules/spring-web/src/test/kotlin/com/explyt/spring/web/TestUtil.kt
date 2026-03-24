/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference

object TestUtil {

    inline fun <reified T> PsiElement.findTypedReferenceAt(offset: Int): T? {
        val reference = findReferenceAt(offset)
        val typedReference = reference as? T
        if (typedReference != null) return typedReference

        val multiReference = reference as? PsiMultiReference ?: return null
        return multiReference.references.asSequence()
            .mapNotNull { it as? T }
            .firstOrNull()
    }

}