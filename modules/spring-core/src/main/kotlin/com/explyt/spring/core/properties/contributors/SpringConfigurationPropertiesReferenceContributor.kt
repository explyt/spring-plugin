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
