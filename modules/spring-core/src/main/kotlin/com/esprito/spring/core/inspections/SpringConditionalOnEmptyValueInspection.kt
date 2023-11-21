package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.CONDITIONAL_ON_PROPERTY
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod


class SpringConditionalOnEmptyValueInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun checkMethod(
        method: PsiMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        return check(method, manager, isOnTheFly)
    }

    override fun checkClass(
        aClass: PsiClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (aClass.isAnnotationType) {
            return null
        }
        return check(aClass, manager, isOnTheFly)
    }

    private fun check(
        member: PsiMember,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val module = ModuleUtilCore.findModuleForPsiElement(member) ?: return null
        val service = SpringSearchService.getInstance(module.project)

        val metaHolder = service.getMetaAnnotations(module, CONDITIONAL_ON_PROPERTY)
        val psiAnnotation = member.annotations.firstOrNull { metaHolder.contains(it) } ?: return null

        val annotationMemberValues = metaHolder.getAnnotationMemberValues(member, setOf("value", "name"))

        return if (annotationMemberValues.isEmpty()) {
            arrayOf(
                manager.createProblemDescriptor(
                    psiAnnotation,
                    psiAnnotation.getHighlightRange(),
                    SpringCoreBundle.message("esprito.spring.inspection.conditionalOnProperty.empty"),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly
                )
            )
        } else {
            null
        }
    }

}