package com.esprito.spring.web.providers

import com.esprito.spring.core.util.UastUtil.getArgumentValueAsEnumName
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
        val psiElement = uExpression.sourcePsi ?: return emptyArray()
        val uCallExpression = uExpression.uastParent as? UCallExpression ?: return emptyArray()
        val callReceiver = uCallExpression.receiver ?: return emptyArray()

        val uMethodCall = callReceiver.getUCallExpression() ?: return emptyArray()
        var requestMethod = uMethodCall.methodName ?: return emptyArray()
        if (requestMethod == "method") {
            requestMethod = uMethodCall.getArgumentValueAsEnumName(0) ?: return emptyArray()
        }

        val text = ElementManipulators.getValueText(psiElement)
        if (text.isBlank()) return emptyArray()

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