/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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