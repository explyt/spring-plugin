package com.esprito.jpa.model

import com.intellij.psi.PsiType

sealed interface JpaEntityAttributeType {
    object Unknown: JpaEntityAttributeType

    data class Scalar(val psiType: PsiType) : JpaEntityAttributeType

    data class Entity(val jpaEntity: JpaEntity) : JpaEntityAttributeType
}