package com.esprito.spring.web.providers

import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.references.EspritoControllerMethodReference
import com.esprito.spring.web.util.SpringWebUtil
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression

class UrlPathControllerMethodReferenceProvider : UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val psiElement = uExpression.sourcePsi ?: return PsiReference.EMPTY_ARRAY
        val uCallExpression = uExpression.uastParent as? UCallExpression ?: return PsiReference.EMPTY_ARRAY
        val psiMethod = uCallExpression.resolve() ?: return PsiReference.EMPTY_ARRAY
        val urlTemplateIndex = SpringWebUtil.getUrlTemplateIndex(psiMethod)
        if (!isAtIndexIn(uCallExpression, urlTemplateIndex, uExpression)) return PsiReference.EMPTY_ARRAY
        if (psiMethod
                .containingClass
                ?.qualifiedName != SpringWebClasses.MOCK_MVC_REQUEST_BUILDERS
        ) return PsiReference.EMPTY_ARRAY

        var requestMethod: String? = (uExpression.uastParent as? UCallExpression)
            ?.methodName
            ?.uppercase() ?: return PsiReference.EMPTY_ARRAY
        if (requestMethod in METHODS_WITH_TYPE) {
            val httpMethodIndex = psiMethod.parameterList
                .parameters
                .indexOfFirst { it.name in SpringWebUtil.HTTP_METHOD_NAMES }
            requestMethod = if (httpMethodIndex < 0) {
                null
            } else {
                uCallExpression
                    .getArgumentForParameter(httpMethodIndex)?.asSourceString()
                    ?.split('.')
                    ?.last()
            }
        }

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

    private fun isAtIndexIn(
        callExpression: UCallExpression,
        urlTemplateIndex: Int,
        argumentExpression: UExpression
    ): Boolean {
        val expressionAtIndex = callExpression.valueArguments.getOrNull(urlTemplateIndex)

        return expressionAtIndex == argumentExpression
    }

    companion object {
        private val METHODS_WITH_TYPE = listOf("MULTIPART", "REQUEST")
    }

}