package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.DEPENDS_ON
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.util.EspritoAnnotationUtil.getMemberValues
import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod


class SpringDependsOnBeanInspection : AbstractBaseJavaLocalInspectionTool() {

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
        return check(aClass, manager, isOnTheFly)
    }

    private fun check(
        member: PsiMember,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val module = ModuleUtilCore.findModuleForPsiElement(member) ?: return null
        val service = SpringSearchService.getInstance(module.project)
        val beanNames = service.getAllBeanNames(module)

        return if (member.isMetaAnnotatedBy(DEPENDS_ON)) {
            member.getMetaAnnotation(DEPENDS_ON)
                .getMemberValues("value").asSequence()
                .filter {
                    !beanNames.contains(
                        AnnotationUtil.getStringAttributeValue(it)
                    )
                }
                .map {
                    manager.createProblemDescriptor(
                        it,
                        it.textRangeInParent.shiftLeft(it.textRangeInParent.startOffset),
                        SpringCoreBundle.message("esprito.spring.inspection.bean.dependsOn"),
                        ProblemHighlightType.ERROR,
                        isOnTheFly
                    )
                }.toList().toTypedArray()
        } else {
            null
        }
    }

}