/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.util.ExplytPsiUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiMember
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElement

object HttpExchanges {

    private val URL_ATTRIBUTES = setOf("url", "value")
    private val METHOD_ATTRIBUTES = setOf("method")
    private val ACCEPT_ATTRIBUTES = setOf("accept")
    private val CONTENT_TYPE_ATTRIBUTES = setOf("contentType")

    sealed interface ExchangeMapping

    data class Mapped(val urls: List<String>, val methods: List<String>) : ExchangeMapping

    data object Conflicting : ExchangeMapping

    private data class Exchange(
        val url: String,
        val methods: List<String>,
        val accept: Set<String>,
        val contentType: List<String>,
    )

    fun exchangeOf(owner: PsiMember, exchangeMah: MetaAnnotationsHolder): ExchangeMapping {
        val annotated = owner.toUElement() as? UAnnotated ?: return declaredExchangeOf(owner, exchangeMah)
        val exchanges = annotated.uAnnotations
            .filter { exchangeMah.contains(it) }
            .map { mergedExchangeOf(it, exchangeMah) }
        val readable = exchanges.filterNotNull().distinct()
        return when {
            readable.size > 1 -> Conflicting
            readable.size == 1 && null !in exchanges -> readable.single().let { Mapped(listOf(it.url), it.methods) }
            else -> declaredExchangeOf(owner, exchangeMah)
        }
    }

    private fun mergedExchangeOf(annotation: UAnnotation, exchangeMah: MetaAnnotationsHolder): Exchange? {
        if (annotation.attributeValues.any { attribute -> hasUnreadableString(attribute.expression) }) return null

        fun values(attributes: Set<String>): List<String>? =
            exchangeMah.getAnnotationMemberValues(annotation, attributes)
                .mapNotNull { if (it.isLibraryValue()) it.libraryStringValue() else it.evaluateString() ?: return null }
                .filter { it.isNotBlank() }

        return Exchange(
            url = (values(URL_ATTRIBUTES) ?: return null).firstOrNull().orEmpty(),
            methods = values(METHOD_ATTRIBUTES) ?: return null,
            accept = values(ACCEPT_ATTRIBUTES)?.toSet() ?: return null,
            contentType = values(CONTENT_TYPE_ATTRIBUTES) ?: return null,
        )
    }

    private fun hasUnreadableString(expression: UExpression): Boolean =
        MetaAnnotationsHolder.getValues(expression).any { it.evaluate() == null && it !is UClassLiteralExpression }

    private fun UExpression.libraryStringValue(): String? =
        evaluateString() ?: (javaPsi as? PsiAnnotationMemberValue)?.let { AnnotationUtil.getStringAttributeValue(it) }

    private fun UExpression.isLibraryValue() = sourcePsi.let { it == null || it is PsiCompiledElement }

    @Suppress("DEPRECATION")
    private fun declaredExchangeOf(owner: PsiMember, exchangeMah: MetaAnnotationsHolder): Mapped {
        val urlValues = exchangeMah.getAnnotationMemberValues(owner, URL_ATTRIBUTES)
        return Mapped(
            urls = if (urlValues.isEmpty()) listOf("")
            else urlValues.mapNotNull { AnnotationUtil.getStringAttributeValue(it) },
            methods = exchangeMah.getAnnotationMemberValues(owner, METHOD_ATTRIBUTES)
                .map { ExplytPsiUtil.getUnquotedText(it) },
        )
    }
}
