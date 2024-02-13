package com.esprito.spring.web.providers

import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.references.EspritoControllerMethodReference
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UPolyadicExpression

class UrlPathControllerMethodReferenceProvider : UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val psiElement = uExpression.sourcePsi ?: return PsiReference.EMPTY_ARRAY
        if (uExpression !is ULiteralExpression && uExpression !is UPolyadicExpression) return PsiReference.EMPTY_ARRAY
        if ((uExpression.uastParent as? UCallExpression)?.resolve()
                ?.containingClass
                ?.qualifiedName != SpringWebClasses.MOCK_MVC_REQUEST_BUILDERS
        ) return PsiReference.EMPTY_ARRAY

        val requestMethod = (uExpression.uastParent as? UCallExpression)
            ?.methodName
            ?.uppercase() ?: return PsiReference.EMPTY_ARRAY

        val text = ElementManipulators.getValueText(psiElement)
        if (text.isBlank()) return PsiReference.EMPTY_ARRAY

        return arrayOf(
            EspritoControllerMethodReference(
                host,
                text,
                requestMethod,
                ElementManipulators.getValueTextRange(psiElement)
            )
        )
    }

}