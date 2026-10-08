/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

object BeanFactoryMethods {
    fun of(psiClass: PsiClass): Sequence<PsiMethod> {
        val visited = mutableSetOf<PsiClass>()
        val methodNames = mutableSetOf<String>()
        val methods = mutableListOf<PsiMethod>()

        fun visit(current: PsiClass) {
            if (current.qualifiedName?.startsWith("java.") == true || !visited.add(current)) return
            ProgressManager.checkCanceled()
            current.methods.forEach { method ->
                if (method.name !in methodNames && method.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) {
                    methodNames += method.name
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
