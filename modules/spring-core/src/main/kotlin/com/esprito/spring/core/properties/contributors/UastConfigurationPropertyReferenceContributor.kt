package com.esprito.spring.core.properties.contributors

import com.esprito.spring.core.properties.providers.ConditionalOnConfigurationPropertyReferenceProvider
import com.esprito.spring.core.properties.providers.GetPropertyMethodPropertyReferenceProvider
import com.esprito.spring.core.properties.providers.ValueConfigurationPropertyReferenceProvider
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.uast.callExpression
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider

class UastConfigurationPropertyReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(
            injectionHostUExpression(),
            ValueConfigurationPropertyReferenceProvider()
        )
        registrar.registerUastReferenceProvider(
            injectionHostUExpression(),
            ConditionalOnConfigurationPropertyReferenceProvider()
        )

        val propertyResolverClass = PsiJavaPatterns.psiClass()
            .inheritorOf(false, "org.springframework.core.env.PropertyResolver")

        val methodCall =
            callExpression()
                .withMethodNames(setOf("getProperty", "containsProperty", "getRequiredProperty"))
                .withReceiver(propertyResolverClass)

        val injectionHostInsideGetPropertyMethod = injectionHostUExpression(false)
            .callParameter(0, methodCall)

        registrar.registerUastReferenceProvider(
            injectionHostInsideGetPropertyMethod,
            GetPropertyMethodPropertyReferenceProvider()
        )


    }
}