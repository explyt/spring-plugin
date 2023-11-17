package com.esprito.spring.core.properties

import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*

class UastConfigurationPropertyReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(injectionHostUExpression(), ValueConfigurationPropertyReferenceProvider())
        registrar.registerUastReferenceProvider(injectionHostUExpression(), ConditionalOnConfigurationPropertyReferenceProvider())
    }
}