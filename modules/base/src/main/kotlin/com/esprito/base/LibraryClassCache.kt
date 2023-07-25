package com.esprito.base

import com.esprito.util.CacheKeyStore
import com.intellij.codeInsight.JavaLibraryModificationTracker
import com.intellij.openapi.application.runReadAction
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
        val javaPsiFacade = JavaPsiFacade.getInstance(project)

        CachedValuesManager.getManager(project)
            .getCachedValue(
                project,
                keys.getValue(fqn),
                {
                    val result = javaPsiFacade.findClass(
                        fqn,
                        LibraryScopeCache.getInstance(project).librariesOnlyScope
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
}