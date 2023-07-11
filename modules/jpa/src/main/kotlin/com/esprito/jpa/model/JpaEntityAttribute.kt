package com.esprito.jpa.model

import com.intellij.psi.PsiType

interface JpaEntityAttribute {
    val name: String

    val type: PsiType
}