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