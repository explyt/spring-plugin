package com.esprito.spring.web.inspections

import com.esprito.inspection.SpringBaseLocalInspectionTool
import com.esprito.spring.web.SpringWebBundle
import com.esprito.spring.web.util.OpenApiFileHelper
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
        if (!OpenApiFileHelper.INSTANCE.isSuitableFile(virtualFile, file)) return ProblemDescriptor.EMPTY_ARRAY

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
                SpringWebBundle.message("esprito.spring.web.inspection.openapi.inspection.version", text),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.WARNING,
            )
        )
    }
}