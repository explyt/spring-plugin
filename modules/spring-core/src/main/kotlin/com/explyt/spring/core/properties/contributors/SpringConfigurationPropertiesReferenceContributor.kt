package com.explyt.spring.core.properties.contributors

import com.explyt.spring.core.properties.providers.SpringConfigurationPropertiesValueReferenceProvider
import com.explyt.spring.core.properties.providers.SpringConfigurationPropertiesValueResourceReferenceProvider
import com.explyt.spring.core.properties.providers.SpringConfigurationPropertyKeyReferenceProvider
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class SpringConfigurationPropertiesReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // key
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PropertyKeyImpl::class.java),
            SpringConfigurationPropertyKeyReferenceProvider()
        )

        // value
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PropertyValueImpl::class.java),
            SpringConfigurationPropertiesValueReferenceProvider()
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PropertyValueImpl::class.java),
            SpringConfigurationPropertiesValueResourceReferenceProvider()
        )
    }
}
