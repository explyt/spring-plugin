/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass

object SpringBeanOwner {

    fun isProcessedBySpring(module: Module, psiClass: PsiClass): Boolean {
        if (!psiClass.isValid) return false
        return psiClass.isSpringTestClass() || isActiveBean(module, psiClass)
    }

    fun PsiClass.isSpringTestClass(): Boolean = isMetaAnnotatedBy(SpringCoreClasses.CONTEXT_CONFIGURATION)

    private fun isActiveBean(module: Module, psiClass: PsiClass): Boolean {
        val qualifiedName = psiClass.qualifiedName
        return SpringSearchServiceFacade.getInstance(module.project).getAllActiveBeans(module)
            .any { it.psiClass.qualifiedName == qualifiedName }
    }
}
