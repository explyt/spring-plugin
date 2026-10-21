/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.InheritanceUtil

object SpringBeanOwner {

    private val TEST_CONTEXT_ANNOTATIONS = listOf(
        SpringCoreClasses.CONTEXT_CONFIGURATION,
        SpringCoreClasses.BOOTSTRAP_WITH,
    )

    fun isProcessedBySpring(module: Module, psiClass: PsiClass): Boolean {
        if (!psiClass.isValid) return false
        return psiClass.isSpringTestClass() || psiClass in activeBeanTypeHierarchy(module)
    }

    fun PsiClass.isSpringTestClass(): Boolean = isMetaAnnotatedBy(TEST_CONTEXT_ANNOTATIONS)

    private fun activeBeanTypeHierarchy(module: Module): Set<PsiClass> =
        CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                collectActiveBeanTypeHierarchy(module),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }

    private fun collectActiveBeanTypeHierarchy(module: Module): Set<PsiClass> {
        val hierarchy = HashSet<PsiClass>()
        SpringSearchServiceFacade.getInstance(module.project).getAllActiveBeans(module).forEach { bean ->
            val beanClass = bean.psiClass
            if (hierarchy.add(beanClass)) {
                InheritanceUtil.getSuperClasses(beanClass, hierarchy, true)
            }
        }
        return hierarchy
    }
}
