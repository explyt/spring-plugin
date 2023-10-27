package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.LOOKUP
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.util.EspritoAnnotationUtil.getMetaAnnotationMemberValues
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiMethod


class SpringLookupBeanInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun checkMethod(
        method: PsiMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val module = ModuleUtilCore.findModuleForPsiElement(method) ?: return null
        val returnClassQN = method.returnType?.resolvedPsiClass?.qualifiedName ?: return null
        val service = SpringSearchService.getInstance(module.project)
        val beanByNames = service.getAllBeanByNames(module)

        val problems = mutableListOf<ProblemDescriptor>()

        method.getMetaAnnotationMemberValues(LOOKUP)?.forEach {
            val lookupValue = AnnotationUtil.getStringAttributeValue(it)
            val psiBeans = beanByNames[lookupValue]

            if (lookupValue.isNullOrBlank()) return@forEach
            if (psiBeans.isNullOrEmpty()) {
                problems += manager.createProblemDescriptor(
                    it,
                    it.getHighlightRange(),
                    SpringCoreBundle.message("esprito.spring.inspection.bean.lookup.unknown", lookupValue),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly
                )
            } else if (!psiBeans.any { bean -> bean.psiClass.qualifiedName == returnClassQN }) {
                problems += manager.createProblemDescriptor(
                    it,
                    it.getHighlightRange(),
                    SpringCoreBundle.message("esprito.spring.inspection.bean.lookup.wrongType",
                        returnClassQN,
                        psiBeans[0].psiClass.qualifiedName.toString()),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly
                )
            }
        }
        return problems.toTypedArray()
    }

}