package com.explyt.spring.web.references.contributors

import com.explyt.spring.core.util.UastUtil
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.providers.WebClientUrlPathControllerMethodReferenceProvider
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.uast.UExpressionPattern
import com.intellij.patterns.uast.uExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider
import org.jetbrains.kotlin.idea.completion.or
import org.jetbrains.uast.UExpression


class UastWebClientUrlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(
            uExpression().asFirstParamOfUriCall(SpringWebClasses.WEB_CLIENT_URI_SPEC)
                .or(
                    uExpression().asFirstParamOfUriCall(SpringWebClasses.WEB_TEST_CLIENT_URI_SPEC)
                )
                .or(
                    uExpression().withUastParent(
                        UastUtil.UPolyadicExpressionPattern()
                            .asFirstParamOfUriCall(SpringWebClasses.WEB_CLIENT_URI_SPEC)
                    )
                )
                .or(
                    uExpression().withUastParent(
                        UastUtil.UPolyadicExpressionPattern()
                            .asFirstParamOfUriCall(SpringWebClasses.WEB_TEST_CLIENT_URI_SPEC)
                    )
                ),
            WebClientUrlPathControllerMethodReferenceProvider()
        )
    }

    private fun <T : UExpression, Self : UExpressionPattern<T, Self>> UExpressionPattern<T, Self>.asFirstParamOfUriCall(
        classFqn: String
    ) =
        methodCallParameter(
            0,
            PsiJavaPatterns.psiMethod()
                .definedInClass(classFqn)
                .withName("uri"),
            true
        )

}
