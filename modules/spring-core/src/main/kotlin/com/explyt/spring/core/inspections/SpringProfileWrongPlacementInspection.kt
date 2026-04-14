/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses.BEAN
import com.explyt.spring.core.SpringCoreClasses.COMPONENT
import com.explyt.spring.core.SpringCoreClasses.PROFILE
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.toSourcePsi
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiMember
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod


class SpringProfileWrongPlacementInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val psiMethod = uMethod.javaPsi
        if (psiMethod.isMetaAnnotatedBy(BEAN)) {
            return null
        }
        return check(psiMethod, manager, isOnTheFly)
    }

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val psiClass = uClass.javaPsi
        if (uClass.isAnnotationType) {
            return null
        }
        if (psiClass.isMetaAnnotatedBy(COMPONENT)) {
            return null
        }
        return check(psiClass, manager, isOnTheFly)
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
            .mapNotNull { it.toSourcePsi() }
            .map { annotation ->
                manager.createProblemDescriptor(
                    annotation,
                    annotation.getHighlightRange(),
                    SpringCoreBundle.message("explyt.spring.inspection.profile.wrongPlacement"),
                    ProblemHighlightType.WARNING,
                    isOnTheFly
                )

            }
            .toList()
            .toTypedArray()
    }

}