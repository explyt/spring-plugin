package com.explyt.spring.web.references.contributors

import com.explyt.spring.web.providers.ControllerEndpointToTemplateReferenceProvider
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider

class ControllerEndpointReturnReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(
            injectionHostUExpression(false),
            ControllerEndpointToTemplateReferenceProvider()
        )
    }

}