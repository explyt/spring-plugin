package com.explyt.spring.web.references.contributors

import com.explyt.spring.web.providers.SpringOpenApiJsonUrlEndpointReferenceProvider
import com.explyt.spring.web.util.PlatformPatternUtils
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class SpringOpenApiJsonUrlEndpointReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JsonStringLiteral::class.java)
                .inFile(PlatformPatternUtils.openApiJsonFile()),
            SpringOpenApiJsonUrlEndpointReferenceProvider()
        )
    }

}