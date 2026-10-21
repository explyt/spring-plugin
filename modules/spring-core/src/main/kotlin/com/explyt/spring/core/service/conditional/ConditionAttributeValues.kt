/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.explyt.util.ExplytAnnotationUtil.computeConstantExpression
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiType
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

internal val UExpression.stringValue: String? get() = constantValue as? String

internal val UExpression.booleanValue: Boolean? get() = constantValue as? Boolean

internal val UExpression.classQualifiedName: String?
    get() = classLiteralType?.resolveBeanPsiClass?.qualifiedName

internal val UExpression.enumConstantName: String?
    get() = (resolvedTarget as? PsiEnumConstant)?.name

private val UExpression.constantValue: Any?
    get() = evaluate() ?: memberValue?.computeConstantExpression()

private val UExpression.classLiteralType: PsiType?
    get() = (this as? UClassLiteralExpression)?.type
        ?: (memberValue as? PsiClassObjectAccessExpression)?.operand?.type

private val UExpression.resolvedTarget: PsiElement?
    get() = (this as? UResolvable)?.resolve() ?: (memberValue as? PsiReference)?.resolve()

private val UExpression.memberValue: PsiAnnotationMemberValue?
    get() = (sourcePsi ?: javaPsi) as? PsiAnnotationMemberValue

