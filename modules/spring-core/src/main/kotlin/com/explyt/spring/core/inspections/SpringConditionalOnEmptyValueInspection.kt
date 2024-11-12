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
import com.explyt.spring.core.SpringCoreClasses.CONDITIONAL_ON_PROPERTY
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.toSourcePsi
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UMethod


class SpringConditionalOnEmptyValueInspection : SpringBaseUastLocalInspectionTool() {

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
        if (uClass.isAnnotationType) {
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

        val metaHolder = service.getMetaAnnotations(module, CONDITIONAL_ON_PROPERTY)
        val psiAnnotation = uMember.uAnnotations.asSequence()
            .mapNotNull { it.javaPsi }
            .firstOrNull { metaHolder.contains(it) }
            ?: return null

        val annotationMemberValues = metaHolder.getAnnotationMemberValues(psiAnnotation, setOf("value", "name"))

        return if (annotationMemberValues.isEmpty()) {
            val sourcePsi = psiAnnotation.toSourcePsi() ?: return null
            arrayOf(
                manager.createProblemDescriptor(
                    sourcePsi,
                    sourcePsi.getHighlightRange(),
                    SpringCoreBundle.message("explyt.spring.inspection.conditionalOnProperty.empty"),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly
                )
            )
        } else {
            null
        }
    }

}