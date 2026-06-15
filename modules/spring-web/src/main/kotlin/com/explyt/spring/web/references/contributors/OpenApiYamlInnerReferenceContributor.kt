/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.references.contributors

import com.explyt.spring.web.providers.OpenApiYamlInnerReferenceProvider
import com.explyt.spring.web.util.PlatformPatternUtils
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class OpenApiYamlInnerReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatternUtils.openApiYamlInnerRef(),
            OpenApiYamlInnerReferenceProvider()
        )
    }

}