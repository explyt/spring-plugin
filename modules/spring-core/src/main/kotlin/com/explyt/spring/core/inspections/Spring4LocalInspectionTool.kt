/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.spring.core.util.SpringBootUtil
import com.intellij.psi.PsiFile

/**
 * Base class for inspections that only apply to Spring Boot 4+ projects.
 *
 * The Spring Boot version is checked once per file in [isAvailableForFile] (instead of on every visited PSI element),
 * so the inspection never builds a visitor or walks PSI in projects that are not on Spring Boot 4+.
 */
abstract class Spring4LocalInspectionTool : SpringBaseLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return super.isAvailableForFile(file) && SpringBootUtil.isAtLeastSpringBoot4(file)
    }
}
