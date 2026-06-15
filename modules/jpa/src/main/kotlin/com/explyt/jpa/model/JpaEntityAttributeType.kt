/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.model

import com.intellij.psi.PsiType

sealed interface JpaEntityAttributeType {
    object Unknown : JpaEntityAttributeType

    data class Scalar(val psiType: PsiType) : JpaEntityAttributeType

    data class Entity(val jpaEntity: JpaEntity) : JpaEntityAttributeType
}