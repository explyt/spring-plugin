package com.esprito.spring.core.util

import com.intellij.psi.*
import com.intellij.psi.util.parentOfType

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

}