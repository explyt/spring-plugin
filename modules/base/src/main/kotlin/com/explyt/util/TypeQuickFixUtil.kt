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

package com.explyt.util

import com.explyt.base.LibraryClassCache
import com.intellij.codeInsight.daemon.impl.analysis.HighlightFixUtil
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiImmediateClassType
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter

object TypeQuickFixUtil {

    fun getQuickFixes(uParameter: UParameter, expectedEntityType: PsiType?): Array<LocalQuickFix> {
        if (expectedEntityType == null) return emptyArray()
        val psiVariable = uParameter.javaPsi as? PsiVariable ?: return emptyArray<LocalQuickFix>()
        if (psiVariable.language != JavaLanguage.INSTANCE) return emptyArray()
        val fixes = HighlightFixUtil.getChangeVariableTypeFixes(psiVariable, expectedEntityType)
        return fixes.filterIsInstance<LocalQuickFix>().toTypedArray()
    }

    fun getQuickFixesReturnType(uMethod: UMethod, vararg types: PsiType): Array<LocalQuickFix> {
        val psiMethod = uMethod.javaPsi
        if (psiMethod.language != JavaLanguage.INSTANCE) return emptyArray()
        return types.map { QuickFixFactory.getInstance().createMethodReturnFix(psiMethod, it, false) }
            .toTypedArray()
    }

    fun wrapToCollection(
        project: Project,
        targetType: PsiType?
    ): PsiType? {
        if (targetType == null) return null
        val collection = LibraryClassCache.searchForLibraryClass(project, Collection::class.java.name) ?: return null
        val typeParameters: Array<PsiTypeParameter> = collection.typeParameters
        var substitutor = PsiSubstitutor.EMPTY
        if (typeParameters.size == 1) {
            substitutor = substitutor.put(typeParameters[0], targetType)
        }
        return PsiImmediateClassType(collection, substitutor)
    }

    fun getUnwrapTargetType(type: PsiType): PsiType? {
        if (type is PsiArrayType) return type.componentType
        val genericParameters = (type as? PsiClassType)?.parameters ?: return type
        if (genericParameters.isEmpty()) return type
        if (genericParameters.size > 1) return null
        return genericParameters[0]
    }

}