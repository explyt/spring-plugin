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