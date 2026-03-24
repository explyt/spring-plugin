/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses.DEPENDS_ON
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.evaluateString


class SpringDependsOnBeanInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        return check(uMethod, manager, isOnTheFly)
    }

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        return check(uClass, manager, isOnTheFly)
    }

    private fun check(
        uMember: UDeclaration,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val member = uMember.javaPsi ?: return null
        val module = ModuleUtilCore.findModuleForPsiElement(member) ?: return null
        val service = SpringSearchServiceFacade.getInstance(module.project)
        val beanNames = service.getAllBeanByNames(module)

        val metaHolder = SpringSearchService.getInstance(module.project).getMetaAnnotations(module, DEPENDS_ON)
        val uAnnotation = uMember.uAnnotations.firstOrNull { metaHolder.contains(it) } ?: return null

        val annotationMemberValues = metaHolder.getAnnotationMemberValues(uAnnotation, setOf("value"))

        return annotationMemberValues.asSequence()
            .filter {
                val valueString = it.evaluateString()?.takeIf { it.isNotBlank() } ?: return@filter false
                !beanNames.contains(valueString)
            }
            .mapNotNull {
                val sourcePsi = it.sourcePsi ?: return@mapNotNull null

                manager.createProblemDescriptor(
                    sourcePsi,
                    sourcePsi.getHighlightRange(),
                    SpringCoreBundle.message("explyt.spring.inspection.bean.dependsOn.incorrect"),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly
                )
            }.toList().toTypedArray()
    }

}