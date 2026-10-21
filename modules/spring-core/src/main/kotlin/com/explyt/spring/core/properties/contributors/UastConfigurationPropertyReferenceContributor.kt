/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.contributors

import com.explyt.spring.core.properties.providers.ConditionalOnConfigurationPropertyReferenceProvider
import com.explyt.spring.core.properties.providers.GetPropertyMethodPropertyReferenceProvider
import com.explyt.spring.core.properties.providers.ValueConfigurationPropertyReferenceProvider
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns.string
import com.intellij.patterns.uast.callExpression
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.patterns.uast.uAnnotationQualifiedNamePattern
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider

class UastConfigurationPropertyReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Spring resolves ${...} through the embedded value resolver in string attributes of its
        // own annotations (@KafkaListener topics/groupId, @RequestMapping paths, @Scheduled cron,
        // ...), not only in @Value, so an attribute list per annotation would go stale with every
        // Spring release. The provider extracts nothing from a literal without ${, and the pattern
        // unwraps array initializers, so topics = ["..."] maps to the topics attribute.
        registrar.registerUastReferenceProvider(
            injectionHostUExpression().annotationParams(
                uAnnotationQualifiedNamePattern(string().startsWith(SPRING_ANNOTATION_PACKAGE_PREFIX)),
                string()
            ),
            ValueConfigurationPropertyReferenceProvider(),
            PsiReferenceRegistrar.LOWER_PRIORITY
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

    companion object {
        private const val SPRING_ANNOTATION_PACKAGE_PREFIX = "org.springframework."
    }
}
