package com.explyt.jpa.model

import com.intellij.psi.PsiElement

interface JpaEntity {
    val isValid: Boolean
    val psiElement: PsiElement?

    val name: String?

    val attributes: List<JpaEntityAttribute>

    val isPersistent: Boolean

    fun findAttribute(name: String): JpaEntityAttribute? = attributes.firstOrNull { it.name == name }
}