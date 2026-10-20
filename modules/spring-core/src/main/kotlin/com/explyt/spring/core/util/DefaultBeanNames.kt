/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.intellij.psi.PsiClass

object DefaultBeanNames {

    fun of(psiClass: PsiClass): String? = shortNameOf(psiClass)?.let(::uncapitalizeAsProperty)

    fun uncapitalizeAsProperty(name: String): String = when {
        name.isEmpty() -> name
        name.length > 1 && name[0].isUpperCase() && name[1].isUpperCase() -> name
        else -> name[0].lowercaseChar() + name.substring(1)
    }

    private fun shortNameOf(psiClass: PsiClass): String? {
        val name = psiClass.name ?: return null
        val outer = psiClass.containingClass ?: return name
        return shortNameOf(outer)?.let { "$it.$name" }
    }
}
