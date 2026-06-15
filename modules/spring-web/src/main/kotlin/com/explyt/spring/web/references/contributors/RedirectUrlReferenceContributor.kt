/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.references.contributors

import com.explyt.spring.web.providers.RedirectUrlControllerMethodReferenceProvider
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider


class RedirectUrlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(
            injectionHostUExpression(false),
            RedirectUrlControllerMethodReferenceProvider()
        )
    }

}