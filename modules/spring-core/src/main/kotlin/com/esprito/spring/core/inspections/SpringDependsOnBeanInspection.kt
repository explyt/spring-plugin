package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.DEPENDS_ON
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.util.EspritoAnnotationUtil.getMemberValues
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiAnnotationMemberValue
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
        val beanNames = service.getAllBeanByNames(module)

        val metaHolder = service.getMetaAnnotations(module, DEPENDS_ON)
        val targetMethods = setOf("value")

        val annotationMemberValues = mutableListOf<PsiAnnotationMemberValue>()
        for (annotation in member.annotations) {
            val annotationFqn = annotation.qualifiedName ?: continue
            if (!metaHolder.contains(annotation)) continue

            annotationMemberValues +=
                annotation.attributes.asSequence()
                    .filter {
                        metaHolder.isAttributeRelatedWith(
                            annotationFqn,
                            it.attributeName,
                            DEPENDS_ON,
                            targetMethods
                        )
                    }
                    .flatMap { annotation.getMemberValues(it.attributeName) }
        }

        return annotationMemberValues.asSequence()
            .filter {
                !beanNames.contains(
                    AnnotationUtil.getStringAttributeValue(it)
                )
            }
            .map {
                manager.createProblemDescriptor(
                    it,
                    it.getHighlightRange(),
                    SpringCoreBundle.message("esprito.spring.inspection.bean.dependsOn.incorrect"),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly
                )
            }.toList().toTypedArray()
    }

}