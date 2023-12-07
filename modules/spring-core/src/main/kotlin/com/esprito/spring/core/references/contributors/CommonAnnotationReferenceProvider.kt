package com.esprito.spring.core.references.contributors

import com.esprito.spring.core.service.SpringSearchService
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.*

abstract class CommonAnnotationReferenceProvider(private val searchAttributes: AnnotationSearchAttributes) :
    UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val uNamedExpression = uExpression.getParentOfType<UNamedExpression>() ?: return PsiReference.EMPTY_ARRAY
        val uAnnotation = uExpression.getParentOfType<UAnnotation>() ?: return PsiReference.EMPTY_ARRAY
        val psiAnnotation = uAnnotation.javaPsi ?: return PsiReference.EMPTY_ARRAY
        val psiAnnotationFqn = psiAnnotation.qualifiedName ?: return PsiReference.EMPTY_ARRAY

        val attributeName = uNamedExpression.name ?: "value"
        val module = ModuleUtilCore.findModuleForPsiElement(psiAnnotation) ?: return PsiReference.EMPTY_ARRAY
        val searchService = SpringSearchService.getInstance(psiAnnotation.project)

        for ((annotationFqn, attributes) in searchAttributes) {
            val metaHolder = searchService.getMetaAnnotations(module, annotationFqn)
            if (metaHolder.contains(psiAnnotation)
                && metaHolder.isAttributeRelatedWith(psiAnnotationFqn, attributeName, annotationFqn, attributes)
            ) {
                val valueText = uExpression.evaluateString() ?: return PsiReference.EMPTY_ARRAY
                val literalExpressionPsi = uExpression.sourcePsi ?: return PsiReference.EMPTY_ARRAY

                val rangeInElement = TextRange(
                    0,
                    valueText.length
                ).shiftRight(ElementManipulators.getValueTextRange(literalExpressionPsi).startOffset)
                return arrayOf(
                    getReference(host, valueText, rangeInElement)
                )
            }
        }

        return PsiReference.EMPTY_ARRAY
    }

    abstract fun getReference(
        host: PsiLanguageInjectionHost,
        valueText: String,
        rangeInElement: TextRange
    ): PsiReference

}

typealias AnnotationSearchAttributes = Map<String, Set<String>>

