/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier

object BeanFactoryMethods {
    fun of(psiClass: PsiClass): Sequence<PsiMethod> {
        val visited = mutableSetOf<PsiClass>()
        val factoryKeys = mutableSetOf<Pair<String, String>>()
        val methods = mutableListOf<PsiMethod>()

        fun visit(current: PsiClass) {
            if (current.qualifiedName?.startsWith("java.") == true || !visited.add(current)) return
            ProgressManager.checkCanceled()
            current.methods.forEach { method ->
                if (current.isInterface && method.hasModifierProperty(PsiModifier.ABSTRACT)) return@forEach
                if (method.isMetaAnnotatedBy(SpringCoreClasses.BEAN)
                    && factoryKeys.add(method.resolveBeanName.first() to method.name)
                ) {
                    methods += method
                }
            }
            current.interfaces.forEach(::visit)
            current.superClass?.let(::visit)
        }

        visit(psiClass)
        return methods.asSequence()
    }
}
