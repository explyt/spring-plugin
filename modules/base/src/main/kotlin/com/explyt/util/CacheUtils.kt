package com.explyt.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

object CacheUtils {

    fun <T> getCachedValue(
        module: Module,
        modificationTracker: ModificationTracker?,
        supplier: () -> T
    ): T {
        val cacheManager = CachedValuesManager.getManager(module.project)

        return cacheManager.getCachedValue(module, cacheManager.getKeyForClass(supplier::class.java), {
            CachedValueProvider.Result(
                supplier.invoke(), modificationTracker
            )
        }, false)
    }

    fun <T> getCachedValue(
        project: Project,
        modificationTracker: ModificationTracker?,
        supplier: () -> T
    ): T {
        val cacheManager = CachedValuesManager.getManager(project)

        return cacheManager.getCachedValue(project, cacheManager.getKeyForClass(supplier::class.java), {
            CachedValueProvider.Result(
                supplier.invoke(), modificationTracker
            )
        }, false)
    }

}