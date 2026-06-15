/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.providers

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.properties.references.ExplytLibraryPropertyReference
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.util.ExplytAnnotationUtil.getStringMemberValues
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
        if (uExpression !is ULiteralExpression && uExpression !is UPolyadicExpression) return EMPTY_ARRAY

        val namedExpression = uExpression.getParentOfType<UNamedExpression>() ?: return EMPTY_ARRAY
        val uAnnotation = uExpression.getParentOfType<UAnnotation>() ?: return EMPTY_ARRAY
        val psiAnnotation = uAnnotation.javaPsi ?: return EMPTY_ARRAY
        val annotationQn = psiAnnotation.qualifiedName ?: return EMPTY_ARRAY
        val module = ModuleUtilCore.findModuleForPsiElement(psiAnnotation) ?: return EMPTY_ARRAY
        val attributeName = namedExpression.name ?: "value"

        val annotationHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_PROPERTY)
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
                    ExplytLibraryPropertyReference(host, "", TextRange.from(1, 0), prefixValue)
                } else {
                    val startOffset = host.text.indexOf(value)
                    if (startOffset == -1) return@mapNotNull null

                    val range = TextRange.allOf(value)

                    ExplytLibraryPropertyReference(
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
