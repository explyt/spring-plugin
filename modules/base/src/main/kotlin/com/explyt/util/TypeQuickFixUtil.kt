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