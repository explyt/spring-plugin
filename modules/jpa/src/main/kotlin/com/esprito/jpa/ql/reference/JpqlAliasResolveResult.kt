package com.esprito.jpa.ql.reference

import com.esprito.jpa.ql.psi.JpqlAliasDeclaration
import com.intellij.psi.PsiElement

class JpqlAliasResolveResult(
    val alias: JpqlAliasDeclaration
) : JpqlResolveResult {
    override fun getElement(): PsiElement = alias.identifier

    override fun isValidResult(): Boolean = alias.isValid
}
