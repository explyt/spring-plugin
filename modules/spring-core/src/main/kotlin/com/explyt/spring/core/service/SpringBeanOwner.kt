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
import com.intellij.psi.PsiMethod
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
        if (psiClass.isSpringTestClass()) return true
        val owners = activeBeanOwners(module)
        return psiClass in owners.beanTypeHierarchy || psiClass.isSubtypeOfAny(owners.factoryReturnTypes)
    }

    fun PsiClass.isSpringTestClass(): Boolean = isMetaAnnotatedBy(TEST_CONTEXT_ANNOTATIONS)

    private fun PsiClass.isSubtypeOfAny(types: Set<PsiClass>): Boolean =
        types.isNotEmpty() && !InheritanceUtil.processSupers(this, false) { it !in types }

    private class ActiveBeanOwners(
        val beanTypeHierarchy: Set<PsiClass>,
        val factoryReturnTypes: Set<PsiClass>,
    )

    private fun activeBeanOwners(module: Module): ActiveBeanOwners =
        CachedValuesManager.getManager(module.project).getCachedValue(module) {
            val trackers = ModificationTrackerManager.getInstance(module.project)
            CachedValueProvider.Result(
                collectActiveBeanOwners(module),
                trackers.getUastModelAndLibraryTracker(),
                trackers.getExternalSystemTracker(),
            )
        }

    private fun collectActiveBeanOwners(module: Module): ActiveBeanOwners {
        val hierarchy = HashSet<PsiClass>()
        val factoryReturnTypes = HashSet<PsiClass>()
        SpringSearchServiceFacade.getInstance(module.project).getAllActiveBeans(module).forEach { bean ->
            val beanClass = bean.psiClass
            if (bean.psiMember is PsiMethod) factoryReturnTypes += beanClass
            if (hierarchy.add(beanClass)) {
                InheritanceUtil.getSuperClasses(beanClass, hierarchy, true)
            }
        }
        return ActiveBeanOwners(hierarchy, factoryReturnTypes)
    }
}
