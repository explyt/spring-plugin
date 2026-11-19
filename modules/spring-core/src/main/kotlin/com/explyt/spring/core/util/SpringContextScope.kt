/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.search.GlobalSearchScope

object SpringContextScope {

    fun of(module: Module): GlobalSearchScope {
        val runners = ModuleUtilCore.getAllDependentModules(module) + module
        return GlobalSearchScope.union(runners.map { it.moduleWithDependenciesScope })
    }
}
