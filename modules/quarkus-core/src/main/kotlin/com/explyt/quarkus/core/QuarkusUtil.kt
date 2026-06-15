/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.quarkus.core

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.util.SpringCoreUtil.canResolveBeanClass
import com.explyt.spring.core.util.SpringCoreUtil.getPsiType
import com.explyt.spring.core.util.SpringCoreUtil.isEqualOrInheritorBeanType
import com.explyt.spring.core.util.SpringCoreUtil.matchesWildcardType
import com.explyt.util.ExplytPsiUtil.allSupers
import com.explyt.util.ExplytPsiUtil.isList
import com.explyt.util.ExplytPsiUtil.isNonPrivate
import com.explyt.util.ExplytPsiUtil.isObject
import com.explyt.util.ExplytPsiUtil.isOptional
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.explyt.util.ExplytPsiUtil.returnPsiClass
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod

object QuarkusUtil {

    fun isQuarkusProject(psiElement: PsiElement): Boolean {
        return LibraryClassCache.searchForLibraryClass(psiElement.project, QuarkusCoreClasses.CORE_CLASS) != null
    }

    fun isQuarkusModule(psiElement: PsiElement): Boolean {
        return ModuleUtilCore.findModuleForPsiElement(psiElement)
            ?.let {
                JavaPsiFacade.getInstance(psiElement.project)
                    .findClass(QuarkusCoreClasses.CORE_CLASS, it.moduleWithLibrariesScope) != null
            } == true
    }

    fun isBeanCandidateClass(psiClass: PsiClass): Boolean {
        return psiClass.isValid
                && !psiClass.isInterface
                && psiClass !is PsiTypeParameter
                && psiClass.isNonPrivate
                && !psiClass.isAnnotationType
                && psiClass.qualifiedName != null
                && !PsiUtil.isLocalOrAnonymousClass(psiClass)
    }

    fun getBeanClass(uBeanElement: UElement): PsiClass? {
        return when (uBeanElement) {
            is UMethod -> uBeanElement.returnPsiClass
            is UField -> uBeanElement.returnPsiClass
            is UClass -> uBeanElement.javaPsi
            else -> null
        }
    }

    fun PsiField.isCandidateQuarkus(
        targetType: PsiType?,
        targetClasses: Set<PsiClass>,
        targetClass: PsiClass
    ): Boolean {
        if (targetType == this.type) return true
        if (targetType == null && targetClass.qualifiedName == this.type.resolvedPsiClass?.qualifiedName) return true
        if (targetType != null && targetType.isEqualOrInheritorBeanType(this.type)) return true
        if (targetType != null && this.type.isAssignableFrom(targetType)) return true

        if (targetType is PsiArrayType) {
            return false
        }

        val isResolved = this.type.canResolveBeanClassQuarkus(targetClasses, targetClass)
        if (!isResolved) return false
        if (targetType == null) return true

        if (targetType !is PsiClassType) return true
        val psiClassType = getPsiType(this)
        return if (targetType.parameters.isNotEmpty()) {
            psiClassType?.isEqualOrInheritorBeanType(targetType) == true
        } else true
    }

    private fun PsiType.canResolveBeanClassQuarkus(
        targetClasses: Set<PsiClass>,
        targetClass: PsiClass? = null
    ): Boolean {
        val psiType = beanPsiTypeQuarkus
        return when (psiType) {
            is PsiClassType -> psiType.resolvedPsiClass.canResolveBeanClass(targetClasses)
            is PsiWildcardType -> {
                if (!psiType.isBounded && !psiType.extendsBound.isObject) {
                    return true
                }
                if (psiType.isSuper && targetClass != null) {
                    return psiType.superBound.resolvedPsiClass?.allSupers()?.any { it == targetClass } == true
                }
                targetClasses.any { it.matchesWildcardType(psiType) }
            }

            else -> false
        }
    }

    private val PsiType.beanPsiTypeQuarkus: PsiType?
        get() {
            if (this is PsiArrayType) {
                return null
            }
            if (this !is PsiClassType) {
                return null
            }
            if (isList) {
                // Collection<Bean>
                return parameters.firstOrNull() ?: this
            }
            if (isOptional) {
                // Optional<Bean>
                return parameters.firstOrNull() ?: return this
            }

            return this
        }
}