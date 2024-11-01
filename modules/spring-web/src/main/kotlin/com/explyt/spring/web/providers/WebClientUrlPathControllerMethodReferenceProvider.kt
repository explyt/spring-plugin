package com.explyt.spring.web.providers

import com.explyt.spring.core.util.UastUtil.getArgumentValueAsEnumName
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.*

class WebClientUrlPathControllerMethodReferenceProvider : UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val uFullExpression = uExpression.uastParent as? UPolyadicExpression ?: uExpression
        val uCallExpression = uFullExpression.uastParent as? UCallExpression ?: return emptyArray()
        val callReceiver = uCallExpression.receiver ?: return emptyArray()

        val uMethodCall = callReceiver.getUCallExpression() ?: return emptyArray()
        var requestMethod = uMethodCall.methodName ?: return emptyArray()
        if (requestMethod == "method") {
            requestMethod = uMethodCall.getArgumentValueAsEnumName(0) ?: return emptyArray()
        }

        val text = uFullExpression.evaluateString() ?: return emptyArray()
        if (text.isBlank()) return emptyArray()

        return arrayOf(
            ExplytControllerMethodReference(
                host,
                text,
                requestMethod.uppercase(),
                ElementManipulators.getValueTextRange(host)
            )
        )
    }

}