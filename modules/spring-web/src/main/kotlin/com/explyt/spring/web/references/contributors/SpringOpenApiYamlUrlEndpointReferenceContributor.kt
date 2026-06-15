/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.references.contributors

import com.explyt.spring.web.providers.SpringOpenApiYamlUrlEndpointReferenceProvider
import com.explyt.spring.web.util.PlatformPatternUtils
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.jetbrains.yaml.psi.YAMLKeyValue

class SpringOpenApiYamlUrlEndpointReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLKeyValue::class.java)
                .inFile(PlatformPatternUtils.openApiYamlFile()),
            SpringOpenApiYamlUrlEndpointReferenceProvider()
        )
    }

}