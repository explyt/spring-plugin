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

package com.explyt.spring.core.util

import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.util.childrenOfType
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.toUElement

object PsiAnnotationUtils {

    fun getTypeNames(annotationMemberValues: Set<PsiAnnotationMemberValue>): Set<String> {
        return annotationMemberValues.asSequence()
            .flatMap { it.childrenOfType<PsiTypeElement>() }
            .map { it.type }
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