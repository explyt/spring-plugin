package com.esprito.spring.core.references.contributors

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.references.EspritoBeanReference
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.*

class UastBeanReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(injectionHostUExpression(), BeanReferenceProvider())
    }
}

class BeanReferenceProvider : UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val expression = uExpression as? ULiteralExpression ?: return PsiReference.EMPTY_ARRAY
        val uNamedExpression = expression.getParentOfType<UNamedExpression>() ?: return PsiReference.EMPTY_ARRAY
        val uAnnotation = expression.getParentOfType<UAnnotation>() ?: return PsiReference.EMPTY_ARRAY
        val psiAnnotation = uAnnotation.javaPsi ?: return PsiReference.EMPTY_ARRAY
        val psiAnnotationClass = psiAnnotation.resolveAnnotationType() ?: return PsiReference.EMPTY_ARRAY

        // check if an expression within right place
        annotationToBeanProperties
            .filter { (annotation, _) ->
                annotation == uAnnotation.qualifiedName || psiAnnotationClass.isMetaAnnotatedBy(annotation)
            }
            .flatMap { (_, beanProperties) -> beanProperties }
            .firstOrNull { it == uNamedExpression.name || uNamedExpression.name == null && it == "value" }
            ?: return PsiReference.EMPTY_ARRAY

        val valueText = expression.evaluateString() ?: return PsiReference.EMPTY_ARRAY

        val literalExpressionPsi = expression.javaPsi ?: return PsiReference.EMPTY_ARRAY
        val rangeInElement = TextRange(0, valueText.length).shiftRight(ElementManipulators.getValueTextRange(literalExpressionPsi).startOffset)
        return arrayOf(
            EspritoBeanReference(host, valueText, rangeInElement)
        )
    }

    companion object {
        val annotationToBeanProperties = mapOf(
            SpringCoreClasses.DEPENDS_ON to listOf("value"),
            SpringCoreClasses.LOOKUP to listOf("value"),
            SpringCoreClasses.CACHEABLE to listOf("value", "cacheNames", "keyGenerator", "cacheManager", "cacheResolver"),
            SpringCoreClasses.CONDITIONAL_ON_BEAN to listOf("name"),
            SpringCoreClasses.BEAN to listOf("value", "name")
        )

    }
}


