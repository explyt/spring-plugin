/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.contributors

import com.explyt.spring.core.SpringProperties.LOGGING_LEVEL
import com.explyt.spring.core.properties.providers.SpringConfigurationPropertiesKeyLoggingLevelReferenceProvider
import com.explyt.spring.core.properties.providers.SpringConfigurationPropertiesValueReferenceProvider
import com.explyt.spring.core.properties.providers.SpringConfigurationPropertiesValueResourceReferenceProvider
import com.explyt.spring.core.properties.providers.SpringConfigurationPropertyKeyReferenceProvider
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

class SpringConfigurationPropertiesReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // key
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PropertyKeyImpl::class.java),
            SpringConfigurationPropertyKeyReferenceProvider()
        )

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PropertyKeyImpl::class.java)
                .with(object : PatternCondition<PropertyKeyImpl>(LOGGING_LEVEL) {
                    override fun accepts(key: PropertyKeyImpl, context: ProcessingContext): Boolean {
                        return key.text.startsWith("$LOGGING_LEVEL.")
                    }
                }),
            SpringConfigurationPropertiesKeyLoggingLevelReferenceProvider()
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
