package com.esprito.util

import com.intellij.patterns.uast.UExpressionPattern
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.psi.registerUastReferenceProvider
import org.jetbrains.uast.UExpression

object EspritoContributorUtil {

    fun addAnnotationValueContributor(
        registrar: PsiReferenceRegistrar,
        injection: UExpressionPattern<UExpression, *>,
        className: String,
        provider: UastInjectionHostReferenceProvider
    ) {
        addAnnotationValueContributor(registrar, injection, className, provider, listOf("value"))
    }

    fun addAnnotationValueContributor(
        registrar: PsiReferenceRegistrar,
        injection: UExpressionPattern<UExpression, *>,
        className: String,
        provider: UastInjectionHostReferenceProvider,
        parameterNames: List<String>
    ) {
        parameterNames.forEach {
            registrar.registerUastReferenceProvider(
                injection.annotationParam(className, it), provider, PsiReferenceRegistrar.LOWER_PRIORITY
            )
        }
    }
}