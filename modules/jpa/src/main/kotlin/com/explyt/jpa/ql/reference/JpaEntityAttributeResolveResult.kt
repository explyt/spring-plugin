/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.ql.reference

import com.explyt.jpa.model.JpaEntityAttribute
import com.intellij.psi.PsiElement

class JpaEntityAttributeResolveResult(
    val entityAttribute: JpaEntityAttribute
) : JpqlResolveResult {
    override fun getElement(): PsiElement? = entityAttribute.psiElement

    override fun isValidResult(): Boolean = entityAttribute.isValid
}