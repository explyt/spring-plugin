package com.esprito.spring.core.properties.contributors

import com.esprito.spring.core.properties.providers.SpringConfigurationPropertiesValueReferenceProvider
import com.esprito.spring.core.properties.providers.SpringConfigurationPropertiesValueResourceReferenceProvider
import com.esprito.spring.core.properties.providers.SpringConfigurationPropertyKeyReferenceProvider
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.jetbrains.yaml.psi.YAMLKeyValue

class SpringYamlConfigurationPropertiesReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLKeyValue::class.java),
            SpringConfigurationPropertyKeyReferenceProvider()
        )

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLKeyValue::class.java),
            SpringConfigurationPropertiesValueReferenceProvider()
        )

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLKeyValue::class.java),
            SpringConfigurationPropertiesValueResourceReferenceProvider()
        )
    }
}
