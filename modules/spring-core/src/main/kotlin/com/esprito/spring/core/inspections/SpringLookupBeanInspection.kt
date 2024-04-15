package com.esprito.spring.core.inspections

import com.esprito.inspection.SpringBaseUastLocalInspectionTool
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.LOOKUP
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.util.EspritoAnnotationUtil.getMetaAnnotationMemberValues
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.esprito.util.EspritoPsiUtil.toSourcePsi
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
        val service = SpringSearchService.getInstance(module.project)
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
                    SpringCoreBundle.message("esprito.spring.inspection.bean.lookup.unknown", lookupValue),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly
                )
            } else if (!psiBeans.any { bean -> bean.psiClass.qualifiedName == returnClassQN }) {
                problems += manager.createProblemDescriptor(
                    valueSourcePsi,
                    valueSourcePsi.getHighlightRange(),
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