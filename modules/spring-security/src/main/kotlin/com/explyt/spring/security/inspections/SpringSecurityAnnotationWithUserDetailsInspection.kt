/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.security.inspections

import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.security.SpringSecurityBundle
import com.explyt.spring.security.SpringSecurityClasses
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.toSourcePsi
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UMethod

class SpringSecurityAnnotationWithUserDetailsInspection : AbstractBaseUastLocalInspectionTool() {
    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        return check(uMethod, manager, isOnTheFly)
    }

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        return check(uClass, manager, isOnTheFly)
    }

    private fun check(
        member: UDeclaration,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()

        if (member.annotations.isEmpty()) {
            return emptyArray()
        }
        val module = ModuleUtilCore.findModuleForPsiElement(member) ?: return emptyArray()
        val metaHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringSecurityClasses.WITH_USER_DETAILS)
        val psiAnnotation = member.uAnnotations.asSequence()
            .mapNotNull { it.javaPsi }
            .firstOrNull { metaHolder.contains(it) }
            ?: return emptyArray()

        val details = metaHolder.getAnnotationMemberValues(psiAnnotation, setOf("userDetailsServiceBeanName"))
            .firstOrNull() ?: return emptyArray()
        val beanName = AnnotationUtil.getStringAttributeValue(details) ?: return emptyArray()


        val beanPsiClass = SpringSearchServiceFacade.getInstance(module.project).getAllBeanByNames(module)
            .filter { it.key == beanName }
            .map { it.value.firstOrNull()?.psiClass }
            .firstOrNull()

        val element = details.toSourcePsi() ?: return emptyArray()
        if (beanPsiClass == null) {
            problems += manager.createProblemDescriptor(
                element,
                element.getHighlightRange(),
                SpringSecurityBundle.message("explyt.spring.inspection.bean.error.message", beanName),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                isOnTheFly
            )
        } else {
            if (!InheritanceUtil.isInheritor(beanPsiClass, SpringSecurityClasses.USER_DETAILS_SERVICE)) {
                problems += manager.createProblemDescriptor(
                    element,
                    element.getHighlightRange(),
                    SpringSecurityBundle.message(
                        "explyt.spring.inspection.bean.must.type",
                        SpringSecurityClasses.USER_DETAILS_SERVICE
                    ),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    isOnTheFly
                )
            }
        }
        return problems.toTypedArray()
    }

}