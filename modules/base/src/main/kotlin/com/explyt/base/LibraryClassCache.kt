/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.explyt.util.CacheKeyStore
import com.intellij.java.library.JavaLibraryModificationTracker
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.LibraryScopeCache
import com.intellij.openapi.util.Key
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.FactoryMap

object LibraryClassCache {
    private val keys = FactoryMap.create<String, Key<CachedValue<PsiClass>>> { Key(it) }

    fun searchForLibraryClass(
        project: Project,
        fqn: String
    ): PsiClass? = runReadAction {
        if (DumbService.isDumb(project)) findWhileIndexing(project, fqn) else findCached(project, fqn)
    }

    fun searchForLibraryClass(module: Module, fqn: String) = searchForLibraryClass(module.project, fqn)

    fun searchForLibraryClasses(
        module: Module,
        fqns: Collection<String>
    ): Collection<PsiClass> =
        fqns.mapNotNull { searchForLibraryClass(module, it) }

    private fun findCached(project: Project, fqn: String): PsiClass? =
        CachedValuesManager.getManager(project)
            .getCachedValue(
                project,
                keys.getValue(fqn),
                {
                    CachedValueProvider.Result.create(
                        findLibraryClass(project, fqn),

                        @Suppress("UnstableApiUsage")
                        JavaLibraryModificationTracker.getInstance(project),

                        CacheKeyStore.cacheReset
                    )
                },
                false
            )

    private fun findWhileIndexing(project: Project, fqn: String): PsiClass? =
        project.getUserData(keys.getValue(fqn))?.upToDateOrNull?.get()
            ?: try {
                findLibraryClass(project, fqn)
            } catch (_: IndexNotReadyException) {
                null
            }

    private fun findLibraryClass(project: Project, fqn: String): PsiClass? =
        JavaPsiFacade.getInstance(project).findClass(fqn, LibraryScopeCache.getInstance(project).librariesOnlyScope)
}
