/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.references.contributors

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.references.CompleteBeanReference
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.util.TextRange
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
        val valueLiteral = uExpression as? ULiteralExpression ?: return PsiReference.EMPTY_ARRAY
        val uAnnotation = uExpression.getParentOfType<UAnnotation>() ?: return PsiReference.EMPTY_ARRAY

        val valueText = valueLiteral.evaluateString() ?: return PsiReference.EMPTY_ARRAY
        val literalExpressionPsi = valueLiteral.javaPsi ?: return PsiReference.EMPTY_ARRAY
        val psiAnnotation = uAnnotation.javaPsi ?: return PsiReference.EMPTY_ARRAY
        val psiAnnotationClass = psiAnnotation.resolveAnnotationType() ?: return PsiReference.EMPTY_ARRAY

        if (SpringCoreClasses.QUALIFIERS.none { it == uAnnotation.qualifiedName || psiAnnotationClass.isMetaAnnotatedBy(it) }) {
            return PsiReference.EMPTY_ARRAY
        }
        val resolveAnnotationType = psiAnnotation.resolveAnnotationType() ?: return PsiReference.EMPTY_ARRAY
        if (resolveAnnotationType.fields.isEmpty() && resolveAnnotationType.methods.isEmpty()) return PsiReference.EMPTY_ARRAY

        val rangeInElement = TextRange(0, valueText.length)
            .shiftRight(ElementManipulators.getValueTextRange(literalExpressionPsi).startOffset)
        return arrayOf(CompleteBeanReference(host, valueText, rangeInElement))
    }

}

