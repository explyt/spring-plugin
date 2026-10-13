/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.service.MergedAnnotationAttributes
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.util.childrenOfType
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.toUElement

class ConditionClassReferences(private val holder: MetaAnnotationsHolder, private val carrier: PsiMember) {
    private val readsSource = carrier.language != JavaLanguage.INSTANCE
    private val conditionFqn = holder.getRootClassQualified()

    fun unresolvedClassLiteral(attribute: String): String? =
        if (readsSource) sourceValues(attribute).filterIsInstance<UClassLiteralExpression>()
            .firstOrNull { it.type?.resolvedPsiClass == null }
            ?.let { it.sourcePsi?.text?.removeSuffix(KOTLIN_CLASS_SUFFIX) ?: it.asRenderString() }
        else lightValues(attribute)
            .flatMap { it.childrenOfType<PsiTypeElement>() }
            .firstOrNull { it.type.resolvedPsiClass == null }
            ?.text

    fun classNames(attribute: String): Sequence<String> =
        if (readsSource) sourceValues(attribute).mapNotNull { it.evaluate() as? String }.distinct()
        else lightValues(attribute).mapNotNull { AnnotationUtil.getStringAttributeValue(it) }.distinct()

    private fun lightValues(attribute: String): Sequence<PsiAnnotationMemberValue> =
        conditionAnnotations({ it.annotations.asList() }, { it })
            .mapNotNull { it.findDeclaredAttributeValue(attribute) }
            .flatMap { flatten(it) }

    private fun sourceValues(attribute: String): Sequence<UExpression> =
        conditionAnnotations(::sourceAnnotationsOf, UAnnotation::javaPsi)
            .mapNotNull { it.findDeclaredAttributeValue(attribute) }
            .flatMap { MetaAnnotationsHolder.getValues(it) }

    private fun sourceAnnotationsOf(element: PsiModifierListOwner): List<UAnnotation> =
        (element.toUElement() as? UAnnotated)?.uAnnotations
            ?: element.annotations.mapNotNull { it.toUElement() as? UAnnotation }

    private fun <A : Any> conditionAnnotations(
        annotationsOf: (PsiModifierListOwner) -> List<A>,
        psiOf: (A) -> PsiAnnotation?,
    ): Sequence<A> = sequence {
        val visitedTypes = mutableSetOf<String>()
        val pending = ArrayDeque(annotationsOf(carrier))
        while (pending.isNotEmpty()) {
            ProgressManager.checkCanceled()
            val annotation = pending.removeFirst()
            val psi = psiOf(annotation) ?: continue
            if (!holder.contains(psi)) continue
            if (psi.qualifiedName == conditionFqn) {
                yield(annotation)
                continue
            }
            val type = MergedAnnotationAttributes.annotationTypeOf(psi) ?: continue
            if (visitedTypes.add(type.qualifiedName ?: continue)) pending += annotationsOf(type)
        }
    }

    private fun flatten(value: PsiAnnotationMemberValue): List<PsiAnnotationMemberValue> =
        if (value is PsiArrayInitializerMemberValue) value.initializers.flatMap { flatten(it) } else listOf(value)

    private companion object {
        const val KOTLIN_CLASS_SUFFIX = "::class"
    }
}
