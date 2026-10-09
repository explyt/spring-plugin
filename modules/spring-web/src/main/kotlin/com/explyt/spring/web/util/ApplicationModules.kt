/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.roots.TestModuleProperties
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

object ApplicationModules {

    fun declaresApplication(module: Module): Boolean =
        PackageScanService.getInstance(module.project).applicationClasses(module.moduleScope).isNotEmpty()

    fun servingModulesOf(module: Module): List<Module> =
        CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                findServingModules(module),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker(),
                ProjectRootModificationTracker.getInstance(module.project)
            )
        }

    private fun findServingModules(module: Module): List<Module> {
        if (declaresApplication(module)) return listOf(module)
        val serving = LinkedHashSet<Module>()
        val visited = mutableSetOf(module)
        val pending = ArrayDeque(listOfNotNull(TestModuleProperties.getInstance(module).productionModule))
        pending += ModuleRootManager.getInstance(module).dependencies
        while (pending.isNotEmpty()) {
            ProgressManager.checkCanceled()
            val candidate = pending.removeFirst()
            if (!visited.add(candidate)) continue
            if (declaresApplication(candidate)) {
                serving += candidate
            } else {
                pending += ModuleRootManager.getInstance(candidate).dependencies
            }
        }
        return serving.toList()
    }
}
