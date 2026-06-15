/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.model

import com.intellij.psi.PsiElement

interface JpaEntityAttribute {
    val psiElement: PsiElement?

    val isValid: Boolean

    val name: String?

    val type: JpaEntityAttributeType
}