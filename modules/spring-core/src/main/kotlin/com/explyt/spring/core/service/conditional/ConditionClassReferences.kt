/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.util.childrenOfType
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.toUElement

class ConditionClassReferences(private val holder: MetaAnnotationsHolder, private val carrier: PsiMember) {
    private val readsSource = carrier.language != JavaLanguage.INSTANCE

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

    @Suppress("DEPRECATION")
    private fun lightValues(attribute: String) =
        holder.getAnnotationMemberValues(carrier, setOf(attribute)).asSequence()

    private fun sourceValues(attribute: String): Sequence<UExpression> =
        (carrier.toUElement() as? UAnnotated)?.uAnnotations.orEmpty().asSequence()
            .filter { holder.contains(it) }
            .flatMap { holder.getAnnotationMemberValues(it, setOf(attribute)) }
            .flatMap { MetaAnnotationsHolder.getValues(it) }

    private companion object {
        const val KOTLIN_CLASS_SUFFIX = "::class"
    }
}
