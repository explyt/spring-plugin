/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.util.Computable

/**
 * The launch classpath is assembled from the IDE library roots, and a root may point at a file the build system
 * resolved in its dependency graph but never downloaded. Such a library contributes nothing to the command line
 * and the application dies with a `NoClassDefFoundError` that names a class, never the dependency behind it.
 */
object NativeClasspathValidator {

    fun findLibrariesWithMissingFiles(modules: Array<Module>): List<String> {
        return ApplicationManager.getApplication().runReadAction(Computable {
            modules.flatMapTo(LinkedHashSet()) { librariesWithMissingFiles(it) }.toList()
        })
    }

    private fun librariesWithMissingFiles(module: Module): Set<String> {
        val librariesWithMissingFiles = LinkedHashSet<String>()
        OrderEnumerator.orderEntries(module).recursively().forEachLibrary { library ->
            val invalidRootUrls = (library as? LibraryEx)?.getInvalidRootUrls(OrderRootType.CLASSES)
            if (!invalidRootUrls.isNullOrEmpty()) {
                librariesWithMissingFiles += library.name ?: invalidRootUrls.first()
            }
            true
        }
        return librariesWithMissingFiles
    }
}
