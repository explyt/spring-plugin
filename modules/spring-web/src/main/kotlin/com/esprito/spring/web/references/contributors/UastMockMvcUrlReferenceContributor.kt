package com.esprito.spring.web.references.contributors

import com.esprito.spring.web.providers.UrlPathControllerMethodReferenceProvider
import com.esprito.spring.web.util.SpringWebUtil
import com.intellij.patterns.uast.callExpression
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider


class UastMockMvcUrlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val methodCall =
            callExpression()
                .withMethodNames(SpringWebUtil.REQUEST_METHODS)

        val injectionHostInsideHttpMethods = injectionHostUExpression(false)
            .inCall(methodCall)

        registrar.registerUastReferenceProvider(
            injectionHostInsideHttpMethods,
            UrlPathControllerMethodReferenceProvider()
        )
    }

}