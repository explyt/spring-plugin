package com.esprito.spring.web.references.contributors

import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.providers.WebClientUrlPathControllerMethodReferenceProvider
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider


class UastWebClientUrlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val injection = injectionHostUExpression()

        registrar.registerUastReferenceProvider(
            injection.methodCallParameter(
                0,
                PsiJavaPatterns.psiMethod()
                    .definedInClass(SpringWebClasses.WEB_CLIENT_URI_SPEC)
                    .withName("uri"),
                true
            ),
            WebClientUrlPathControllerMethodReferenceProvider()
        )

        registrar.registerUastReferenceProvider(
            injection.methodCallParameter(
                0,
                PsiJavaPatterns.psiMethod()
                    .definedInClass(SpringWebClasses.WEB_TEST_CLIENT_URI_SPEC)
                    .withName("uri"),
                true
            ),
            WebClientUrlPathControllerMethodReferenceProvider()
        )
    }

}