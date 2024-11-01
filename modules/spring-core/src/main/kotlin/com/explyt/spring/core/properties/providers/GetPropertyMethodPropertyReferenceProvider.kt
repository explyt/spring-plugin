package com.explyt.spring.core.properties.providers

import com.explyt.spring.core.properties.references.ExplytLibraryPropertyReference
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReference.EMPTY_ARRAY
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UPolyadicExpression

class GetPropertyMethodPropertyReferenceProvider : UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val psiElement = uExpression.sourcePsi ?: return EMPTY_ARRAY
        if (uExpression !is ULiteralExpression && uExpression !is UPolyadicExpression) return EMPTY_ARRAY

        val text = ElementManipulators.getValueText(psiElement)
        if (text.isBlank()) return EMPTY_ARRAY

        return arrayOf(
            ExplytLibraryPropertyReference(
                host,
                text,
                ElementManipulators.getValueTextRange(psiElement)
            )
        )
    }

}
