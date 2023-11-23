package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.BEAN
import com.esprito.spring.core.SpringCoreClasses.COMPONENT
import com.esprito.spring.core.SpringCoreClasses.PROFILE
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod


class SpringProfileWrongPlacementInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun checkMethod(
        method: PsiMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (method.isMetaAnnotatedBy(BEAN)) {
            return null
        }
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
        if (aClass.isMetaAnnotatedBy(COMPONENT)) {
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

        val annotationsHolder = service.getMetaAnnotations(module, PROFILE)

        return member.annotations.asSequence()
            .filter { annotationsHolder.contains(it) }
            .map { annotation ->
                manager.createProblemDescriptor(
                    annotation,
                    annotation.getHighlightRange(),
                    SpringCoreBundle.message("esprito.spring.inspection.profile.wrongPlacement"),
                    ProblemHighlightType.WARNING,
                    isOnTheFly
                )

            }
            .toList()
            .toTypedArray()
    }

}