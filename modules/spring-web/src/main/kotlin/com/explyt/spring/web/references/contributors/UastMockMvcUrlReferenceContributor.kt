package com.explyt.spring.web.references.contributors

import com.explyt.spring.core.util.UastUtil
import com.explyt.spring.web.providers.UrlPathControllerMethodReferenceProvider
import com.explyt.spring.web.util.SpringWebUtil
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

        registrar.registerUastReferenceProvider(
            injectionHostUExpression(false)
                .inCall(methodCall)
                .andNot(injectionHostUExpression(false).withUastParent(UastUtil.UPolyadicExpressionPattern())),
            UrlPathControllerMethodReferenceProvider()
        )

        registrar.registerUastReferenceProvider(
            injectionHostUExpression(false)
                .withUastParent(UastUtil.UPolyadicExpressionPattern().inCall(methodCall)),
            UrlPathControllerMethodReferenceProvider()
        )
    }

}