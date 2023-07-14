package com.esprito.jpa.model

import com.intellij.psi.PsiElement

interface JpaEntityAttribute {
    val psiElement: PsiElement?

    val isValid: Boolean

    val name: String?

    val type: JpaEntityAttributeType
}