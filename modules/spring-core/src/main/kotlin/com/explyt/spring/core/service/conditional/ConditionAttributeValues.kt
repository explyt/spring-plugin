/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiMember
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType

internal fun MetaAnnotationsHolder.mergedValuesOf(carrier: PsiMember, attributes: Set<String>): List<UExpression> =
    carrier.annotations.filter { contains(it) }.flatMap { mergedValuesOf(it, attributes) }

internal fun MetaAnnotationsHolder.mergedValuesOf(annotation: PsiAnnotation, attributes: Set<String>): List<UExpression> {
    val source = annotation.toUElementOfType<UAnnotation>() ?: return declaredValuesOf(annotation, attributes)
    return getAnnotationMemberValues(source, attributes).toList()
}

@Suppress("DEPRECATION")
private fun MetaAnnotationsHolder.declaredValuesOf(annotation: PsiAnnotation, attributes: Set<String>): List<UExpression> =
    getAnnotationMemberValues(annotation, attributes)
        .mapNotNull { it.toUElement() as? UExpression }
        .flatMap { MetaAnnotationsHolder.getValues(it) }

internal val UExpression.stringValue: String? get() = evaluate() as? String

internal val UExpression.booleanValue: Boolean? get() = evaluate() as? Boolean

internal val UExpression.classQualifiedName: String?
    get() = (this as? UClassLiteralExpression)?.type?.resolveBeanPsiClass?.qualifiedName

internal val UExpression.enumConstantName: String?
    get() = ((this as? UResolvable)?.resolve() as? PsiEnumConstant)?.name
