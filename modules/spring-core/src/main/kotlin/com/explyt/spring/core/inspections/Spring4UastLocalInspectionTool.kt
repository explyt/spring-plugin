/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.util.SpringBootUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile

/**
 * Base class for UAST inspections that only apply to Spring Boot 4+ projects.
 *
 * The Spring Boot version is checked once per file in [isAvailableForFile] (instead of on every visited PSI element),
 * so the inspection never builds a visitor or walks PSI in projects that are not on Spring Boot 4+.
 */
abstract class Spring4UastLocalInspectionTool : SpringBaseUastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return super.isAvailableForFile(file) && SpringBootUtil.isAtLeastSpringBoot4(file)
    }

    protected fun isClassAvailable(file: PsiFile, fqn: String): Boolean {
        return JavaPsiFacade.getInstance(file.project).findClass(fqn, file.resolveScope) != null
    }

    protected fun isAnyClassAvailable(file: PsiFile, vararg fqns: String): Boolean {
        return fqns.any { isClassAvailable(file, it) }
    }
}
