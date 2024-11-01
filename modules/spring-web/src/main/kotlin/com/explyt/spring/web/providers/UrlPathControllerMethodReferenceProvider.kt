package com.explyt.spring.web.providers

import com.explyt.spring.core.util.UastUtil.getArgumentValueAsEnumName
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.REQUEST_METHODS_WITH_TYPE
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.evaluateString

class UrlPathControllerMethodReferenceProvider : UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val uFullExpression = uExpression.uastParent as? UPolyadicExpression ?: uExpression
        val uCallExpression = uFullExpression.uastParent as? UCallExpression ?: return emptyArray()
        val psiMethod = uCallExpression.resolve() ?: return emptyArray()
        val urlTemplateIndex = SpringWebUtil.getUrlTemplateIndex(psiMethod)
        if (!isAtIndexIn(uCallExpression, urlTemplateIndex, uFullExpression)) return emptyArray()
        if (psiMethod
                .containingClass
                ?.qualifiedName != SpringWebClasses.MOCK_MVC_REQUEST_BUILDERS
        ) return emptyArray()

        var requestMethod: String? = (uFullExpression.uastParent as? UCallExpression)
            ?.methodName
            ?.uppercase() ?: return emptyArray()
        if (requestMethod in REQUEST_METHODS_WITH_TYPE) {
            val httpMethodIndex = SpringWebUtil.getHttpMethodIndex(psiMethod)
            requestMethod = if (httpMethodIndex < 0) {
                null
            } else {
                uCallExpression
                    .getArgumentValueAsEnumName(httpMethodIndex)
            }
        }

        val text = uFullExpression.evaluateString() ?: return emptyArray()
        if (text.isBlank()) return emptyArray()

        return arrayOf(
            ExplytControllerMethodReference(
                host,
                text,
                requestMethod,
                ElementManipulators.getValueTextRange(host)
            )
        )
    }

    private fun isAtIndexIn(
        callExpression: UCallExpression,
        urlTemplateIndex: Int,
        argumentExpression: UExpression
    ): Boolean {
        val expressionAtIndex = callExpression.valueArguments.getOrNull(urlTemplateIndex)

        return expressionAtIndex == argumentExpression
    }

}