package com.esprito.spring.core.properties

import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class SpringConfigurationPropertiesReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyKeyImpl::class.java), SpringConfigurationPropertiesKeyReferenceProvider())
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyValueImpl::class.java), SpringConfigurationPropertiesValueReferenceProvider())
    }
}
