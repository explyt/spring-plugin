package com.esprito.spring.core.references.contributors

import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.references.CompleteBeanReference
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
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

        if (SpringProperties.stringQualifiers.none { it == uAnnotation.qualifiedName || psiAnnotationClass.isMetaAnnotatedBy(it) }) {
            return PsiReference.EMPTY_ARRAY
        }
        val resolveAnnotationType = psiAnnotation.resolveAnnotationType() ?: return PsiReference.EMPTY_ARRAY
        if (resolveAnnotationType.fields.isEmpty() && resolveAnnotationType.methods.isEmpty()) return PsiReference.EMPTY_ARRAY

        val rangeInElement = TextRange(0, valueText.length)
            .shiftRight(ElementManipulators.getValueTextRange(literalExpressionPsi).startOffset)
        return arrayOf(CompleteBeanReference(host, valueText, rangeInElement))
    }

}

