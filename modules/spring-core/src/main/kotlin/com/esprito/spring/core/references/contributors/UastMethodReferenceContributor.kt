package com.esprito.spring.core.references.contributors

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.references.EspritoMethodReference
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.evaluateString

class UastMethodReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val injection = injectionHostUExpression()

        for (annotationToMethods in MethodReferenceProvider.annotationToMethodProperties.entries) {
            val annotation = annotationToMethods.key

            for (property in annotationToMethods.value) {
                registrar.registerUastReferenceProvider(
                    injection.annotationParam(annotation, property),
                    MethodReferenceProvider(),
                    PsiReferenceRegistrar.HIGHER_PRIORITY
                )
            }
        }
    }
}

class MethodReferenceProvider : UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val valueLiteral = uExpression as? ULiteralExpression ?: return PsiReference.EMPTY_ARRAY
        val valueText = valueLiteral.evaluateString() ?: return PsiReference.EMPTY_ARRAY
        val literalExpressionPsi = valueLiteral.javaPsi ?: return PsiReference.EMPTY_ARRAY

        val rangeInElement = TextRange(
            0,
            valueText.length
        ).shiftRight(ElementManipulators.getValueTextRange(literalExpressionPsi).startOffset)
        return arrayOf(
            EspritoMethodReference(host, valueText, rangeInElement)
        )
    }

    companion object {
        val annotationToMethodProperties = mapOf(
            SpringCoreClasses.BEAN to listOf("initMethod", "destroyMethod"),
        )

    }
}


