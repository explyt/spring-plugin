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

