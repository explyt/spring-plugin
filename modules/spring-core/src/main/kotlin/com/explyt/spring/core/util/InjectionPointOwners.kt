/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiClass

object InjectionPointOwners {

    fun of(beanClasses: Sequence<PsiClass>): Set<PsiClass> {
        val owners = LinkedHashSet<PsiClass>()
        val pending = ArrayDeque<PsiClass>()
        beanClasses.filter { it.isValid }.forEach {
            if (owners.add(it)) pending.addLast(it)
        }
        val visited = HashSet<PsiClass>()
        while (pending.isNotEmpty()) {
            ProgressManager.checkCanceled()
            val psiClass = pending.removeFirst()
            if (!visited.add(psiClass)) continue
            psiClass.supers
                .filter { it.isValid && isProjectSource(it) }
                .forEach {
                    owners.add(it)
                    pending.addLast(it)
                }
        }
        return owners
    }

    private fun isProjectSource(psiClass: PsiClass): Boolean {
        val virtualFile = (psiClass.navigationElement.containingFile ?: psiClass.containingFile)?.virtualFile
            ?: return false
        return ProjectFileIndex.getInstance(psiClass.project).isInSourceContent(virtualFile)
    }
}
