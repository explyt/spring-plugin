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
