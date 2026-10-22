/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

object InjectionConstructors {

    fun of(beanClass: PsiClass, isAutowired: (PsiMethod) -> Boolean): Set<PsiMethod> {
        val constructors = beanClass.constructors
        val autowired = constructors.filterTo(LinkedHashSet(), isAutowired)
        if (autowired.isNotEmpty()) return autowired
        val declarations = constructors.groupBy { it.navigationElement }.values
        declarations.singleOrNull()?.let { only ->
            return if (only.any { it.hasParameters() }) only.toSet() else emptySet()
        }
        return kotlinPrimaryNextToNoArgSecondary(declarations)
    }

    private fun kotlinPrimaryNextToNoArgSecondary(declarations: Collection<List<PsiMethod>>): Set<PsiMethod> {
        if (declarations.size != 2) return emptySet()
        val (primary, secondary) = declarations.partition { it.first().isKotlinPrimaryConstructor() }
        val primaryConstructors = primary.singleOrNull() ?: return emptySet()
        val noArgSecondary = secondary.singleOrNull()?.singleOrNull()?.hasParameters() == false
        return if (primaryConstructors.size == 1 && noArgSecondary) primaryConstructors.toSet() else emptySet()
    }

    private fun PsiMethod.isKotlinPrimaryConstructor(): Boolean =
        navigationElement.let { it is KtPrimaryConstructor || it is KtClassOrObject }
}
