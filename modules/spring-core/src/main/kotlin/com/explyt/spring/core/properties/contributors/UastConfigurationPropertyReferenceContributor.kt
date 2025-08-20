/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.properties.contributors

import com.explyt.spring.core.SpringCoreClasses.SCHEDULED
import com.explyt.spring.core.SpringCoreClasses.VALUE
import com.explyt.spring.core.properties.providers.ConditionalOnConfigurationPropertyReferenceProvider
import com.explyt.spring.core.properties.providers.GetPropertyMethodPropertyReferenceProvider
import com.explyt.spring.core.properties.providers.ValueConfigurationPropertyReferenceProvider
import com.explyt.util.ExplytContributorUtil
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.uast.callExpression
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider

class UastConfigurationPropertyReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val injection = injectionHostUExpression()

        ExplytContributorUtil.addAnnotationValueContributor(
            registrar, injection, VALUE, ValueConfigurationPropertyReferenceProvider()
        )
        ExplytContributorUtil.addAnnotationValueContributor(
            registrar, injection, SCHEDULED, ValueConfigurationPropertyReferenceProvider(), VALUE_SCHEDULED
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
        val VALUE_SCHEDULED = listOf("cron", "initialDelayString", "fixedDelayString", "fixedRateString", "zone")
    }
}