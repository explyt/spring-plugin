package com.esprito.jpa.model

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType

interface JpaEntityAttribute {
    val psiElement: PsiElement?

    val isValid: Boolean

    val name: String?

    val type: PsiType
}