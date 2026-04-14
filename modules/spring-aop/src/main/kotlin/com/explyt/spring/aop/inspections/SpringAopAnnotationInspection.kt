/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.aop.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.aop.SpringAopBundle
import com.explyt.spring.aop.SpringAopClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.util.QuickFixUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType.WARNING
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass


class SpringAopAnnotationInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val sourcePsiAspectAnnotation = uClass.uAnnotations
            .find { it.qualifiedName == SpringAopClasses.ASPECT }?.sourcePsi
            ?: return emptyArray()

        val psiElement = uClass.sourcePsi ?: return emptyArray()
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return emptyArray()

        val holderSpringBoot = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.COMPONENT)

        val springBeanAnnotations = uClass.uAnnotations.filter { holderSpringBoot.contains(it) }
        if (springBeanAnnotations.isNotEmpty()) return emptyArray()
        val problemDescriptor = manager.createProblemDescriptor(
            sourcePsiAspectAnnotation, SpringAopBundle.message("explyt.spring.inspection.aop.component"),
            isOnTheFly, QuickFixUtil.addClassAnnotationFix(uClass, SpringCoreClasses.COMPONENT), WARNING
        )
        return arrayOf(problemDescriptor)
    }

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val uClass = method.getContainingUClass() ?: return emptyArray()
        val sourcePsiAspectAnnotation = method.uAnnotations
            .find { it.qualifiedName?.startsWith(SpringAopClasses.BASE_PACKAGE) == true }?.sourcePsi
            ?: return emptyArray()

        val springBeanAnnotations = uClass.uAnnotations.filter { it.qualifiedName == SpringAopClasses.ASPECT }
        if (springBeanAnnotations.isNotEmpty()) return emptyArray()
        val problemDescriptor = manager.createProblemDescriptor(
            sourcePsiAspectAnnotation, SpringAopBundle.message("explyt.spring.inspection.aop.aspect"),
            isOnTheFly, QuickFixUtil.addClassAnnotationFix(uClass, SpringAopClasses.ASPECT), WARNING
        )
        return arrayOf(problemDescriptor)
    }
}
