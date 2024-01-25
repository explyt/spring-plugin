package com.esprito.spring.core.util

import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.parentOfType
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.toUElement

object PsiAnnotationUtils {
    fun getParentAnnotationForPsiLiteralParameter(
        psiElement: PsiElement,
    ): PsiAnnotation? {
        return getParentAnnotationForPsiLiteralParameterCaseOneParameter(psiElement)
            ?: getParentAnnotationForPsiLiteralParameterCaseManyParameters(psiElement)
    }

    fun getParentAnnotationForPsiLiteralParameterCaseOneParameter(
        psiElement: PsiElement,
    ): PsiAnnotation? {
        return (psiElement as? PsiLiteral)
            ?.parentOfType<PsiNameValuePair>()
            ?.parentOfType<PsiAnnotationParameterList>()
            ?.parentOfType<PsiAnnotation>()
    }

    fun getParentAnnotationForPsiLiteralParameterCaseManyParameters(
        psiElement: PsiElement,
    ): PsiAnnotation? {
        return (psiElement as? PsiLiteral)
            ?.parentOfType<PsiArrayInitializerMemberValue>()
            ?.parentOfType<PsiNameValuePair>()
            ?.parentOfType<PsiAnnotationParameterList>()
            ?.parentOfType<PsiAnnotation>()
    }

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