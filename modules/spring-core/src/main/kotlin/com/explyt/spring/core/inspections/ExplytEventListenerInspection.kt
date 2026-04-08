/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses.EVENT_LISTENER
import com.explyt.util.ExplytPsiUtil.getMetaAnnotation
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isPublic
import com.explyt.util.ExplytPsiUtil.toSourcePsi
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiModifier
import com.siyeh.ig.fixes.ChangeModifierFix
import org.jetbrains.uast.UMethod

class ExplytEventListenerInspection : SpringBaseUastLocalInspectionTool() {
    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val psiMethod = uMethod.javaPsi
        if (psiMethod.isMetaAnnotatedBy(EVENT_LISTENER)) {
            var problems = emptyArray<ProblemDescriptor>()
            val eventListenerAnnotation = psiMethod.getMetaAnnotation(EVENT_LISTENER).toSourcePsi() ?: return null
            if (!psiMethod.isPublic) {
                problems +=
                    manager.createProblemDescriptor(
                        eventListenerAnnotation,
                        SpringCoreBundle.message("explyt.spring.inspection.method.eventListener.public"),
                        ChangeModifierFix(PsiModifier.PUBLIC),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        true
                    )
            }
            if (psiMethod.parameterList.parameters.size > 1) {
                problems +=
                    manager.createProblemDescriptor(
                        eventListenerAnnotation,
                        SpringCoreBundle.message("explyt.spring.inspection.method.eventListener.parameters"),
                        null as LocalQuickFix?,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        true
                    )
            }
            return problems
        }
        return null
    }

}