/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.references.contributors

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.references.CompleteBeanReference
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.*

class QualifierReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(injectionHostUExpression(), QualifierReferenceProvider()
        )
    }
}

class QualifierReferenceProvider: UastInjectionHostReferenceProvider() {
    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        if (!uExpression.isPlainStringLiteral()) return PsiReference.EMPTY_ARRAY
        val uAnnotation = uExpression.getParentOfType<UAnnotation>() ?: return PsiReference.EMPTY_ARRAY

        val valueText = uExpression.evaluateString() ?: return PsiReference.EMPTY_ARRAY
        val psiAnnotationClass = uAnnotation.resolve() ?: return PsiReference.EMPTY_ARRAY

        if (SpringCoreClasses.QUALIFIERS.none { it == uAnnotation.qualifiedName || psiAnnotationClass.isMetaAnnotatedBy(it) }) {
            return PsiReference.EMPTY_ARRAY
        }
        if (psiAnnotationClass.fields.isEmpty() && psiAnnotationClass.methods.isEmpty()) return PsiReference.EMPTY_ARRAY

        return arrayOf(CompleteBeanReference(host, valueText, ElementManipulators.getValueTextRange(host)))
    }

    private fun UExpression.isPlainStringLiteral(): Boolean = when (this) {
        is ULiteralExpression -> true
        is UPolyadicExpression -> operands.all { it is ULiteralExpression }
        else -> false
    }
}
