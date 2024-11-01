/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

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