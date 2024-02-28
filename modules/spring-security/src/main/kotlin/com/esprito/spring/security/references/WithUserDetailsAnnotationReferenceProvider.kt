package com.esprito.spring.security.references

import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UPolyadicExpression

class WithUserDetailsAnnotationReferenceProvider : UastInjectionHostReferenceProvider() {
    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val psiElement = uExpression.sourcePsi ?: return PsiReference.EMPTY_ARRAY
        if (uExpression !is ULiteralExpression && uExpression !is UPolyadicExpression) return PsiReference.EMPTY_ARRAY

        val text = ElementManipulators.getValueText(psiElement)
        if (text.isBlank()) return PsiReference.EMPTY_ARRAY

        val range = ElementManipulators.getValueTextRange(psiElement)

        return arrayOf(BeanReference(host, range, text))
    }
}