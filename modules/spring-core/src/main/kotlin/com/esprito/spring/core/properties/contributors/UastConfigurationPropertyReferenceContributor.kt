package com.esprito.spring.core.properties.contributors

import com.esprito.spring.core.properties.providers.ConditionalOnConfigurationPropertyReferenceProvider
import com.esprito.spring.core.properties.providers.ValueConfigurationPropertyReferenceProvider
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider

class UastConfigurationPropertyReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(injectionHostUExpression(), ValueConfigurationPropertyReferenceProvider())
        registrar.registerUastReferenceProvider(injectionHostUExpression(), ConditionalOnConfigurationPropertyReferenceProvider())
    }
}