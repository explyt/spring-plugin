package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.quickfix.KotlinObjectToClassQuickFix
import com.explyt.spring.core.service.SpringSearchUtils
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.uast.UClass


class SpringKotlinObjectInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        if (uClass.lang != KotlinLanguage.INSTANCE) return emptyArray()

        val sourcePsi = uClass.sourcePsi ?: return emptyArray()
        val file = sourcePsi.containingFile ?: return emptyArray()
        val module = ModuleUtilCore.findModuleForFile(file) ?: return emptyArray()
        SpringSearchUtils.findUAnnotation(module, uClass.uAnnotations, SpringCoreClasses.COMPONENT)
            ?: return emptyArray()

        val objectDeclaration = (sourcePsi as? KtObjectDeclaration) ?: return emptyArray()
        val objectKeywordPsi = objectDeclaration.getObjectKeyword() ?: return emptyArray()

        return arrayOf(
            manager.createProblemDescriptor(
                objectKeywordPsi,
                SpringCoreBundle.message("explyt.spring.inspection.kotlin.object.title"),
                isOnTheFly,
                arrayOf(KotlinObjectToClassQuickFix(objectDeclaration)),
                ProblemHighlightType.WARNING
            )
        )
    }
}