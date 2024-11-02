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
import com.explyt.spring.core.SpringCoreClasses.BEAN
import com.explyt.spring.core.SpringCoreClasses.COMPONENT
import com.explyt.spring.core.SpringCoreClasses.CONDITIONAL_ON_BEAN
import com.explyt.spring.core.SpringCoreClasses.CONDITIONAL_ON_CLASS
import com.explyt.spring.core.SpringCoreClasses.CONDITIONAL_ON_MISSING_BEAN
import com.explyt.spring.core.SpringCoreClasses.CONDITIONAL_ON_MISSING_CLASS
import com.explyt.spring.core.SpringCoreClasses.CONDITIONAL_ON_PROPERTY
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.toSourcePsi
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UMethod


class SpringConditionalOnWrongPlacementInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (uMethod.javaPsi.isMetaAnnotatedBy(BEAN)) {
            return null
        }
        return check(uMethod, manager, isOnTheFly)
    }

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (uClass.isAnnotationType) {
            return null
        }
        if (uClass.javaPsi.isMetaAnnotatedBy(COMPONENT)) {
            return null
        }
        return check(uClass, manager, isOnTheFly)
    }

    private fun check(
        uMember: UDeclaration,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val member = uMember.javaPsi ?: return null
        val module = ModuleUtilCore.findModuleForPsiElement(member) ?: return null
        val service = SpringSearchService.getInstance(module.project)

        val annotationsHolders = setOf(
            CONDITIONAL_ON_PROPERTY,
            CONDITIONAL_ON_CLASS,
            CONDITIONAL_ON_MISSING_CLASS,
            CONDITIONAL_ON_BEAN,
            CONDITIONAL_ON_MISSING_BEAN
        ).map { service.getMetaAnnotations(module, it) }

        return uMember.uAnnotations.asSequence()
            .mapNotNull { it.javaPsi }
            .filter { annotation ->
                annotationsHolders.any { it.contains(annotation) }
            }
            .mapNotNull { annotation ->
                val sourcePsi = annotation.toSourcePsi() ?: return@mapNotNull null

                manager.createProblemDescriptor(
                    sourcePsi,
                    sourcePsi.getHighlightRange(),
                    SpringCoreBundle.message("explyt.spring.inspection.conditionalOn.wrongPlacement"),
                    ProblemHighlightType.WARNING,
                    isOnTheFly
                )

            }
            .toList()
            .toTypedArray()
    }

}