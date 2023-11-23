package com.esprito.spring.core.properties

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.util.PropertyUtil
import com.esprito.util.EspritoAnnotationUtil.getStringMemberValues
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReference.EMPTY_ARRAY
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.*

class ConditionalOnConfigurationPropertyReferenceProvider : UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val expression = uExpression as? ULiteralExpression ?: return EMPTY_ARRAY
        val namedExpression = expression.getParentOfType<UNamedExpression>() ?: return EMPTY_ARRAY
        val uAnnotation = expression.getParentOfType<UAnnotation>() ?: return EMPTY_ARRAY
        val psiAnnotation = uAnnotation.javaPsi ?: return EMPTY_ARRAY
        val annotationQn = psiAnnotation.qualifiedName ?: return EMPTY_ARRAY
        val module = ModuleUtilCore.findModuleForPsiElement(psiAnnotation) ?: return EMPTY_ARRAY
        val attributeName = namedExpression.name ?: "value"

        val annotationHolder = MetaAnnotationsHolder.of(module, SpringCoreClasses.CONDITIONAL_ON_PROPERTY)
        if (!annotationHolder.isAttributeRelatedWith(
                annotationQn,
                attributeName,
                SpringCoreClasses.CONDITIONAL_ON_PROPERTY,
                VALUE_FIELDS
            )
        ) return EMPTY_ARRAY

        val values = psiAnnotation.getStringMemberValues(attributeName)

        val prefix = annotationHolder.getAnnotationMemberValues(psiAnnotation, setOf("prefix"))
            .asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .firstOrNull()

        val prefixValue = PropertyUtil.prefixValue(prefix)

        return values
            .mapNotNull { value ->
                if (value.isBlank()) {
                    EspritoLibraryPropertyReference(host, "", TextRange.from(1, 0), prefixValue)
                } else {
                    val startOffset = host.text.indexOf(value)
                    if (startOffset == -1) return@mapNotNull null

                    val range = TextRange.allOf(value)

                    EspritoLibraryPropertyReference(
                        host,
                        value,
                        range.shiftRight(startOffset),
                        prefixValue
                    )
                }
            }.toTypedArray()
    }

    companion object {
        val VALUE_FIELDS = setOf(
            "name",
            "value"
        )
    }

}
