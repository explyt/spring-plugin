package com.explyt.spring.core.providers

import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class SpringAopImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitUsage(element: PsiElement): Boolean {
        if (element is PsiMethod) {
            return !element.isConstructor
                    && element.isAnnotatedBy(IMPLICIT_METHOD_ANNOTATIONS)
        }
        return false

    }

    override fun isImplicitRead(element: PsiElement): Boolean {
        return false
    }

    override fun isImplicitWrite(element: PsiElement): Boolean {
        return false
    }

    companion object {
        private val IMPLICIT_METHOD_ANNOTATIONS = listOf(
            "org.aspectj.lang.annotation.Before",
            "org.aspectj.lang.annotation.After",
            "org.aspectj.lang.annotation.Around",
            "org.aspectj.lang.annotation.AfterReturning",
            "org.aspectj.lang.annotation.AfterThrowing"
        )

    }
}