package com.esprito.util

import com.esprito.util.CacheKeyStore.Companion.getInstance
import com.intellij.codeInsight.JavaLibraryModificationTracker
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import java.util.concurrent.atomic.AtomicBoolean

object ModuleUtil {

    fun isClassAvailableInLibraries(module: Module, fqn: String): Boolean {
        return runReadAction {
            if (module.isDisposed) {
                throw ProcessCanceledException()
            }
            val project = module.project
            val moduleName = module.name
            val key = getInstance(project).getKey<Boolean>(
                "isClassAvailableInLibraries($moduleName, $fqn)"
            )

            CachedValuesManager.getManager(project).getCachedValue(project, key, {
                val resultConsumer = AtomicBoolean(false)
                ModuleRootManager.getInstance(module).orderEntries()
                    .forEachLibrary { library: Library ->
                        if (LibraryUtil.isClassAvailableInLibrary(library, fqn)
                        ) {
                            resultConsumer.set(true)
                            return@forEachLibrary false
                        } else {
                            return@forEachLibrary true
                        }
                    }
                val hasLibraryClass: Boolean = resultConsumer.get()
                CachedValueProvider.Result.create(hasLibraryClass, JavaLibraryModificationTracker.getInstance(project))
            }, false)
        }
    }
}