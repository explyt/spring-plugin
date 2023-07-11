package com.esprito.jpa.model

import com.intellij.psi.PsiElement

interface JpaEntity {
    val psiElement: PsiElement?

    val name: String?

    val attributes: List<JpaEntityAttribute>

    val isValid: Boolean

    fun findAttribute(name: String): JpaEntityAttribute? = attributes.firstOrNull { it.name == name }
}