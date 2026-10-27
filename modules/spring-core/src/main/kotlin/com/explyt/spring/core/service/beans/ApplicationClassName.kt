/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

/**
 * An application class name as a caller writes it.
 *
 * A nested class has two spellings: the binary name `Outer$Inner` that a stack trace, a run configuration or
 * `Class.getName` shows, and the canonical `Outer.Inner` that `PsiClass.getQualifiedName` returns. Both name the same
 * class, so both are accepted.
 */
object ApplicationClassName {

    /** The canonical spelling: `$` separates a nested class in the binary name only. */
    fun canonical(className: String): String = className.replace('$', '.')

    /** Must run under a read action. */
    fun findClass(project: Project, className: String, scope: GlobalSearchScope): PsiClass? {
        val facade = JavaPsiFacade.getInstance(project)
        return facade.findClass(className, scope)
            ?: canonical(className).takeIf { it != className }?.let { facade.findClass(it, scope) }
    }
}
