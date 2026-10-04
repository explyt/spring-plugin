/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement

/**
 * Whether code is the project's own, decided by where it is declared rather than by its package.
 *
 * A package name proves nothing: a sample application, a fork or one of Spring's own repositories lives under
 * `org.springframework.`, and a library can use any package at all. A declaration in a source root of the project -
 * production or test - is project code; one in a library or in the JDK is not.
 */
internal object ProjectSources {

    fun declares(element: PsiElement): Boolean {
        val file = element.navigationElement.containingFile?.virtualFile ?: return false
        return ProjectFileIndex.getInstance(element.project).isInSourceContent(file)
    }
}
