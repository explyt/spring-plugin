package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.BEAN
import com.esprito.spring.core.SpringCoreClasses.COMPONENT
import com.esprito.spring.core.SpringCoreClasses.CONDITIONAL_ON_BEAN
import com.esprito.spring.core.SpringCoreClasses.CONDITIONAL_ON_CLASS
import com.esprito.spring.core.SpringCoreClasses.CONDITIONAL_ON_MISSING_BEAN
import com.esprito.spring.core.SpringCoreClasses.CONDITIONAL_ON_MISSING_CLASS
import com.esprito.spring.core.SpringCoreClasses.CONDITIONAL_ON_PROPERTY
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


class SpringConditionalOnWrongPlacementInspection : AbstractBaseJavaLocalInspectionTool() {

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

        val annotationsHolders = setOf(
            CONDITIONAL_ON_PROPERTY,
            CONDITIONAL_ON_CLASS,
            CONDITIONAL_ON_MISSING_CLASS,
            CONDITIONAL_ON_BEAN,
            CONDITIONAL_ON_MISSING_BEAN
        ).map { service.getMetaAnnotations(module, it) }

        return member.annotations.asSequence()
            .filter { annotation ->
                annotationsHolders.any { it.contains(annotation) }
            }
            .map { annotation ->
                manager.createProblemDescriptor(
                    annotation,
                    annotation.getHighlightRange(),
                    SpringCoreBundle.message("esprito.spring.inspection.conditionalOn.wrongPlacement"),
                    ProblemHighlightType.WARNING,
                    isOnTheFly
                )

            }
            .toList()
            .toTypedArray()
    }

}