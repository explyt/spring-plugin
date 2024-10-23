package com.esprito.spring.web.references.contributors

import com.esprito.spring.web.providers.OpenApiYamlInnerReferenceProvider
import com.esprito.spring.web.util.PlatformPatternUtils
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class OpenApiYamlInnerReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatternUtils.openApiYamlInnerRef(),
            OpenApiYamlInnerReferenceProvider()
        )
    }

}