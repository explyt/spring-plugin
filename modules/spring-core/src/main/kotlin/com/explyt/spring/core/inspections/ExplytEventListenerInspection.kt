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