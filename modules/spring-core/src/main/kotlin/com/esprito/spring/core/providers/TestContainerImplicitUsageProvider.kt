package com.esprito.spring.core.providers

import com.esprito.util.EspritoPsiUtil.isAnnotatedBy
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField

class TestContainerImplicitUsageProvider : ImplicitUsageProvider {

    override fun isImplicitUsage(element: PsiElement): Boolean {
        if (element is PsiField) {
            return element.isAnnotatedBy(IMPLICIT_JUPITER_CONTAINER)
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
        private val IMPLICIT_JUPITER_CONTAINER = "org.testcontainers.junit.jupiter.Container"

    }
}