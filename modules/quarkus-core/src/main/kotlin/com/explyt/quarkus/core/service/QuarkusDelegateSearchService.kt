/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.quarkus.core.service

import com.explyt.base.LibraryClassCache
import com.explyt.quarkus.core.QuarkusCoreClasses.DELEGATE
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.toUElement


@Service(Service.Level.PROJECT)
class QuarkusDelegateSearchService(private val project: Project) {

    fun getDelegateClasses(decoratorClass: UClass): Set<PsiClass> {
        return CachedValuesManager.getManager(project).getCachedValue(decoratorClass) {
            CachedValueProvider.Result(
                decoratorClass.fields.asSequence()
                    .filter { it.isMetaAnnotatedBy(DELEGATE.allFqns) }
                    .mapNotNull { it.type.resolvedPsiClass }
                    .toSet(),
                decoratorClass
            )
        }
    }

    fun allDelegatedClasses(): Set<PsiClass> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                findAllDelegatedClasses(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun findAllDelegatedClasses(): Set<PsiClass> {
        val delegateAnno = LibraryClassCache.searchForLibraryClass(project, DELEGATE.jakarta)
            ?: LibraryClassCache.searchForLibraryClass(project, DELEGATE.javax)
            ?: return emptySet()
        return AnnotatedElementsSearch.searchPsiMembers(delegateAnno, project.projectScope()).asSequence()
            .mapNotNull { it.toUElement() }
            .filterIsInstance<UVariable>()
            .mapNotNull { it.type.resolvedPsiClass }
            .toSet()
    }

    companion object {
        fun getInstance(project: Project): QuarkusDelegateSearchService = project.service()
    }
}