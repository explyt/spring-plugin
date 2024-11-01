package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses.LOOKUP
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.util.ExplytAnnotationUtil.getMetaAnnotationMemberValues
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.returnPsiClass
import com.explyt.util.ExplytPsiUtil.toSourcePsi
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.uast.UMethod


class SpringLookupBeanInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val method = uMethod.javaPsi
        val module = ModuleUtilCore.findModuleForPsiElement(method) ?: return null
        val returnClassQN = method.returnPsiClass?.qualifiedName ?: return null
        val service = SpringSearchServiceFacade.getInstance(module.project)
        val beanByNames = service.getAllBeanByNames(module)

        val problems = mutableListOf<ProblemDescriptor>()

        method.getMetaAnnotationMemberValues(LOOKUP)?.forEach {
            val valueSourcePsi = it.toSourcePsi() ?: return@forEach
            val lookupValue = AnnotationUtil.getStringAttributeValue(it)
            val psiBeans = beanByNames[lookupValue]

            if (lookupValue.isNullOrBlank()) return@forEach
            if (psiBeans.isNullOrEmpty()) {
                problems += manager.createProblemDescriptor(
                    valueSourcePsi,
                    valueSourcePsi.getHighlightRange(),
                    SpringCoreBundle.message("explyt.spring.inspection.bean.lookup.unknown", lookupValue),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly
                )
            } else if (!psiBeans.any { bean -> bean.psiClass.qualifiedName == returnClassQN }) {
                problems += manager.createProblemDescriptor(
                    valueSourcePsi,
                    valueSourcePsi.getHighlightRange(),
                    SpringCoreBundle.message(
                        "explyt.spring.inspection.bean.lookup.wrongType",
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