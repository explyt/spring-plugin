package com.esprito.jpa.ql.reference

import com.esprito.jpa.model.JpaEntity
import com.intellij.psi.PsiElement

class JpaEntityResolveResult(
    val entity: JpaEntity
) : JpqlResolveResult {
    override fun getElement(): PsiElement? = entity.psiElement

    override fun isValidResult(): Boolean = entity.isValid
}