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

package com.explyt.spring.web.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.intellij.codeInsight.daemon.impl.quickfix.MethodReturnTypeFix
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiTypes
import org.jetbrains.uast.UMethod

class SpringInitBinderNonVoidInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val psiMethod = method.javaPsi
        if (!psiMethod.isAnnotatedBy(SpringWebClasses.INIT_BINDER)) return null
        if (psiMethod.returnType == PsiTypes.voidType()) return null
        val identifierElement = method.uastAnchor?.sourcePsi ?: return null

        return arrayOf(
            manager.createProblemDescriptor(
                identifierElement,
                SpringWebBundle.message("explyt.spring.web.inspection.initBinder"),
                isOnTheFly,
                arrayOf(
                    MethodReturnTypeFix(psiMethod, PsiTypes.voidType(), true)
                ),
                ProblemHighlightType.GENERIC_ERROR
            )
        )
    }

}