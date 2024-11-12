/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
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