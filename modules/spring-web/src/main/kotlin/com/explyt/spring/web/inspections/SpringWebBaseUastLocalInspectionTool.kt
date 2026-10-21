/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections

import com.explyt.inspection.InspectionAvailability
import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.web.SpringWebClasses
import com.intellij.psi.PsiFile

abstract class SpringWebBaseUastLocalInspectionTool : SpringBaseUastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return InspectionAvailability.hasLibraryClass(file, SpringWebClasses.WEB_INITIALIZER)
    }
}