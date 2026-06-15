/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiType
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.toUElement

object PsiAnnotationUtils {

    fun getTypeNames(annotationMemberValues: Set<PsiAnnotationMemberValue>): Set<String> {
        return annotationMemberValues.asSequence()
            .mapNotNull { it.toUElement() as? UClassLiteralExpression }
            .mapNotNull { it.type }
            .mapNotNull { it.resolveBeanPsiClass }
            .mapNotNull { it.qualifiedName }
            .toSet()
    }

    fun getPsiTypes(annotationMemberValues: Set<PsiAnnotationMemberValue>): Set<PsiType> {
        return annotationMemberValues.asSequence()
            .mapNotNull { it.toUElement() }
            .filterIsInstance<UClassLiteralExpression>()
            .mapNotNull { it.type }
            .toSet()
    }

}