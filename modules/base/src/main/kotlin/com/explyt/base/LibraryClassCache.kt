/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.explyt.util.CacheKeyStore
import com.intellij.java.library.JavaLibraryModificationTracker
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
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
        CachedValuesManager.getManager(project)
            .getCachedValue(
                project,
                keys.getValue(fqn),
                {
                    val result = JavaPsiFacade.getInstance(project).findClass(
                        fqn, LibraryScopeCache.getInstance(project).librariesOnlyScope
                    )

                    CachedValueProvider.Result.create(
                        result,

                        @Suppress("UnstableApiUsage")
                        JavaLibraryModificationTracker.getInstance(project),

                        CacheKeyStore.cacheReset
                    )
                },
                false
            )
    }

    fun searchForLibraryClass(module: Module, fqn: String) = searchForLibraryClass(module.project, fqn)

    fun searchForLibraryClasses(
        module: Module,
        fqns: Collection<String>
    ): Collection<PsiClass> =
        fqns.mapNotNull { searchForLibraryClass(module, it) }
}