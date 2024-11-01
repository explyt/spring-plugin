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

package com.explyt.jpa.ql.inspection

import com.explyt.jpa.JpaBundle
import com.explyt.jpa.ql.psi.JpqlFullyQualifiedConstructor
import com.explyt.jpa.ql.psi.JpqlVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.GlobalSearchScope

class JpqlFullyQualifiedConstructorInspection : LocalInspectionTool() {

    override fun buildVisitor(
        holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession
    ): PsiElementVisitor = object : JpqlVisitor() {
        override fun visitFullyQualifiedConstructor(fqConstructor: JpqlFullyQualifiedConstructor) {
            val fqConstructorText = fqConstructor.text

            val psiClass = JavaPsiFacade.getInstance(holder.project)
                .findClass(fqConstructorText, GlobalSearchScope.allScope(holder.project))

            if (psiClass != null) return

            if (fqConstructor.identifierList.size == 1) {
                holder.registerProblem(
                    fqConstructor,
                    JpaBundle.message("explyt.jpa.inspection.constructor.not.qualified"),
                    ProblemHighlightType.WARNING
                )
            } else {
                holder.registerProblem(
                    fqConstructor,
                    JpaBundle.message("explyt.jpa.inspection.constructor.entity.not.found", fqConstructorText),
                    ProblemHighlightType.WARNING
                )
            }
        }
    }

}