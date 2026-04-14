/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections

import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.util.OpenApiFileUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

abstract class OpenApiVersionInspectionBase : SpringBaseLocalInspectionTool() {

    abstract fun getProblems(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<ProblemDescriptor> {
        val virtualFile = file.virtualFile ?: return ProblemDescriptor.EMPTY_ARRAY
        if (!OpenApiFileUtil.INSTANCE.isOpenApiFile(virtualFile, file)) return ProblemDescriptor.EMPTY_ARRAY

        return getProblems(file, manager, isOnTheFly)
    }

    fun problemDescriptors(
        manager: InspectionManager,
        element: PsiElement,
        text: String,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        return arrayOf(
            manager.createProblemDescriptor(
                element,
                SpringWebBundle.message("explyt.spring.web.inspection.openapi.inspection.version", text),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.WARNING,
            )
        )
    }
}