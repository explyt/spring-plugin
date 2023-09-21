package com.esprito.spring.core.providers

import com.esprito.spring.core.references.EspritoMethodReference
import com.esprito.spring.core.service.SpringSearchService
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class SpringReferencedMethodUsageProvider : ImplicitUsageProvider {
    override fun isImplicitUsage(element: PsiElement): Boolean {
        if (element !is PsiMethod) return false

        return SpringSearchService.getInstance(element.project)
            .getAllReferencesToElement(element)
            .any { it is EspritoMethodReference }
    }

    override fun isImplicitRead(element: PsiElement): Boolean {
        return false
    }

    override fun isImplicitWrite(element: PsiElement): Boolean {
        return false
    }

}