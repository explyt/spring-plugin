/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.references.contributors

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.references.ExplytMethodReference
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*

class UastMethodReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(injectionHostUExpression(), MethodReferenceProvider())
    }
}

class MethodReferenceProvider : CommonAnnotationReferenceProvider(annotationToMethodProperties) {

    override fun getReference(
        host: PsiLanguageInjectionHost,
        valueText: String,
        rangeInElement: TextRange
    ): PsiReference = ExplytMethodReference(host, valueText, rangeInElement)

    companion object {
        val annotationToMethodProperties = mapOf(
            SpringCoreClasses.BEAN to setOf("initMethod", "destroyMethod"),
        )
    }

}


