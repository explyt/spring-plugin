/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.util

import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade

class MultiVendorClass(subFqn: String) {
    val javax: String = "javax.$subFqn"
    val jakarta: String = "jakarta.$subFqn"

    val allFqns by lazy { listOf(jakarta, javax) }

    fun check(fqn: String?): Boolean = fqn in allFqns

    fun getTargetClass(module: Module?): String {
        module ?: return this.jakarta
        return if (isJakartaModule(module)) this.jakarta else this.javax
    }
    fun isJakartaModule(module: Module): Boolean {
        return JavaPsiFacade.getInstance(module.project)
            .findClass(this.jakarta, module.moduleWithLibrariesScope) != null
    }
}