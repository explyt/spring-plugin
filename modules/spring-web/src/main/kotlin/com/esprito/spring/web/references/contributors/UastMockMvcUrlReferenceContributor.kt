package com.esprito.spring.web.references.contributors

import com.esprito.spring.web.providers.UrlPathControllerMethodReferenceProvider
import com.intellij.patterns.uast.callExpression
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider


class UastMockMvcUrlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val methodCall =
            callExpression()
                .withMethodNames(setOf("get", "head", "post", "put", "patch", "delete", "options", "trace"))

        val injectionHostInsideHttpMethods = injectionHostUExpression(false)
            .callParameter(0, methodCall)

        registrar.registerUastReferenceProvider(
            injectionHostInsideHttpMethods,
            UrlPathControllerMethodReferenceProvider()
        )
    }

}