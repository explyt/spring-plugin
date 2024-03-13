package com.esprito.spring.web.providers

import com.esprito.spring.web.references.EspritoControllerMethodReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UPolyadicExpression


class RedirectUrlControllerMethodReferenceProvider : UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression, host: PsiLanguageInjectionHost, context: ProcessingContext
    ): Array<PsiReference> {
        val psiElement = uExpression.sourcePsi ?: return emptyArray()
        if (uExpression !is ULiteralExpression && uExpression !is UPolyadicExpression) return emptyArray()

        val text = ElementManipulators.getValueText(psiElement)
        val textRange = ElementManipulators.getValueTextRange(psiElement)

        for (prefix in listOf(REDIRECT, FORWARD)) {
            if (!text.startsWith(prefix)) continue

            return arrayOf(
                EspritoControllerMethodReference(
                    host,
                    text.removePrefix(prefix),
                    null,
                    TextRange(textRange.startOffset + prefix.length, textRange.endOffset)
                )
            )
        }

        return emptyArray()
    }

    companion object {
        private const val REDIRECT = "redirect:"
        private const val FORWARD = "forward:"
    }

}