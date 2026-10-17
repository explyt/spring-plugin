/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.core.service.MergedAnnotationAttributes
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.util.ExplytPsiUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.util.text.VersionComparatorUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElement

object HttpExchanges {

    private const val URL = "url"
    private const val VALUE = "value"
    private const val METHOD = "method"
    private val URL_ATTRIBUTES = setOf(URL, VALUE)
    private val METHOD_ATTRIBUTES = setOf(METHOD)
    private val COMPARED_ATTRIBUTES = listOf(URL, METHOD, "contentType", "accept", "headers", "version")
    private const val FIRST_SPRING_FAILING_ON_CONFLICTS = "6.1.4"

    sealed interface ExchangeMapping

    data class Mapped(val urls: List<String>, val methods: List<String>) : ExchangeMapping

    data object Conflicting : ExchangeMapping

    private data class Exchange(val attributes: Map<String, List<String>>) {
        val url get() = attributes[URL].orEmpty().firstOrNull { it.isNotBlank() }.orEmpty()
        val methods get() = attributes[METHOD].orEmpty().filter { it.isNotBlank() }
    }

    fun exchangeOf(owner: PsiMember, exchangeMah: MetaAnnotationsHolder): ExchangeMapping {
        val annotated = owner.toUElement() as? UAnnotated ?: return declaredExchangeOf(owner, exchangeMah)
        val attributes = comparedAttributesOf(owner, exchangeMah)
        if (!conflictsFail(owner)) {
            return firstDirectlyDeclared(annotated, exchangeMah)
                ?.let { mergedExchangeOf(it, attributes, exchangeMah) }
                ?.let { Mapped(listOf(it.url), it.methods) }
                ?: declaredExchangeOf(owner, exchangeMah)
        }
        val exchanges = annotated.uAnnotations
            .filter { exchangeMah.contains(it) }
            .map { mergedExchangeOf(it, attributes, exchangeMah) }
        val readable = exchanges.filterNotNull().distinct()
        return when {
            readable.size > 1 -> Conflicting
            readable.size == 1 && null !in exchanges -> readable.single().let { Mapped(listOf(it.url), it.methods) }
            else -> declaredExchangeOf(owner, exchangeMah)
        }
    }

    private fun firstDirectlyDeclared(annotated: UAnnotated, exchangeMah: MetaAnnotationsHolder): UAnnotation? {
        val exchanges = annotated.uAnnotations.filter { exchangeMah.contains(it) }
        return exchanges.firstOrNull { it.qualifiedName == exchangeMah.getRootClassQualified() } ?: exchanges.firstOrNull()
    }

    private fun conflictsFail(owner: PsiMember): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(owner) ?: return false
        val version = SpringBootUtil.getSpringCoreVersion(module) ?: return false
        return VersionComparatorUtil.compare(version, FIRST_SPRING_FAILING_ON_CONFLICTS) >= 0
    }

    private fun comparedAttributesOf(owner: PsiMember, exchangeMah: MetaAnnotationsHolder): Map<String, PsiMethod?> {
        val exchangeType = JavaPsiFacade.getInstance(owner.project)
            .findClass(exchangeMah.getRootClassQualified(), owner.resolveScope)
            ?: return COMPARED_ATTRIBUTES.associateWith { null }
        val declared = exchangeType.methods.associateBy { it.name }
        return COMPARED_ATTRIBUTES
            .filter { it in declared || it == URL && VALUE in declared }
            .associateWith { declared[it] ?: declared[VALUE] }
    }

    private fun mergedExchangeOf(
        annotation: UAnnotation,
        attributes: Map<String, PsiMethod?>,
        exchangeMah: MetaAnnotationsHolder,
    ): Exchange? {
        if (annotation.attributeValues.any { attribute -> hasUnreadableString(attribute.expression) }) return null

        fun strings(values: Collection<UExpression>): List<String>? =
            values.mapNotNull { if (it.isLibraryValue()) it.libraryStringValue() else it.evaluateString() ?: return null }

        fun values(attribute: String, declaration: PsiMethod?): List<String>? {
            val names = if (attribute == URL) URL_ATTRIBUTES else setOf(attribute)
            val merged = exchangeMah.getAnnotationMemberValues(annotation, names)
            if (merged.isNotEmpty()) return strings(merged)
            val default = declaration?.let(MergedAnnotationAttributes::defaultValueOf) ?: return emptyList()
            return strings(MetaAnnotationsHolder.getValues(default))
        }

        return Exchange(attributes.mapValues { (attribute, declaration) -> values(attribute, declaration) ?: return null })
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
