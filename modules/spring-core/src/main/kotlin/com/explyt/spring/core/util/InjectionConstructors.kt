/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

object InjectionConstructors {

    fun of(beanClass: PsiClass, isAutowired: (PsiMethod) -> Boolean): Set<PsiMethod> {
        val constructors = beanClass.constructors
        val autowired = constructors.filterTo(LinkedHashSet(), isAutowired)
        if (autowired.isNotEmpty()) return autowired
        val onlyDeclaration = constructors.groupBy { it.navigationElement }.values.singleOrNull() ?: return emptySet()
        return if (onlyDeclaration.any { it.hasParameters() }) onlyDeclaration.toSet() else emptySet()
    }
}
