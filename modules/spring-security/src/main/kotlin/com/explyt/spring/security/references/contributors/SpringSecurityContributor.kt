/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.security.references.contributors

import com.explyt.spring.security.references.WithUserDetailsAnnotationReferenceProvider
import com.intellij.patterns.StandardPatterns
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider

class SpringSecurityContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(
            injectionHostUExpression().annotationParam(
                StandardPatterns.string().oneOf("org.springframework.security.test.context.support.WithUserDetails"),
                "userDetailsServiceBeanName"
            ),
            WithUserDetailsAnnotationReferenceProvider()
        )
    }
}