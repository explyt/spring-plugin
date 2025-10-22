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

import com.explyt.spring.core.properties.providers.*
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

class SpringYamlConfigurationPropertiesReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLKeyValue::class.java),
            SpringConfigurationPropertyKeyReferenceProvider()
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            SpringConfigurationYamlKeyLoggingLevelReferenceProvider()
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            DockerEnvVariableYamlReferenceProvider()
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
