package com.esprito.spring.web.providers

import com.esprito.spring.web.references.EspritoControllerMethodReference
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.getUCallExpression

class WebClientUrlPathControllerMethodReferenceProvider : UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val psiElement = uExpression.sourcePsi ?: return PsiReference.EMPTY_ARRAY
        val uCallExpression = uExpression.uastParent as? UCallExpression ?: return PsiReference.EMPTY_ARRAY
        val callReceiver = uCallExpression.receiver ?: return PsiReference.EMPTY_ARRAY

        val requestMethod = callReceiver.getUCallExpression()?.methodName ?: return PsiReference.EMPTY_ARRAY

        val text = ElementManipulators.getValueText(psiElement)
        if (text.isBlank()) return PsiReference.EMPTY_ARRAY

        return arrayOf(
            EspritoControllerMethodReference(
                host,
                text,
                requestMethod.uppercase(),
                ElementManipulators.getValueTextRange(psiElement)
            )
        )
    }

}