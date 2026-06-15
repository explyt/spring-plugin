/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.references.contributors

import com.explyt.spring.core.SpringCoreClasses
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider

class DynamicPropertyRegistryReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val injection = injectionHostUExpression()
        registrar.registerUastReferenceProvider(
            injection.methodCallParameter(
                0,
                PsiJavaPatterns.psiMethod()
                    .definedInClass(SpringCoreClasses.DYNAMIC_PROPERTY_REGISTRY)
                    .withName("add"),
                true
            ),
            DynamicPropertyRegistryReferenceProvider()
        )
    }

}