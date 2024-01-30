package com.esprito.jpa.ql.inspection

import com.esprito.jpa.JpaBundle
import com.esprito.jpa.ql.psi.JpqlFullyQualifiedConstructor
import com.esprito.jpa.ql.psi.JpqlVisitor
import com.esprito.jpa.service.JpaEntitySearch
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor

class JpqlFullyQualifiedConstructorInspection : LocalInspectionTool() {

    override fun buildVisitor(
        holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession
    ): PsiElementVisitor = object : JpqlVisitor() {
        override fun visitFullyQualifiedConstructor(fqConstructor: JpqlFullyQualifiedConstructor) {
            val module = ModuleUtilCore.findModuleForPsiElement(fqConstructor) ?: return
            val fqConstructorText = fqConstructor.text

            for (entity in JpaEntitySearch.getInstance(holder.project).loadEntities(module)) {
                if (fqConstructorText == (entity.psiElement as? PsiClass)?.qualifiedName) return //fqn совпадает, всё верно, выходим

                if (entity.name == fqConstructorText) { //Совпадает только с именем сущности
                    holder.registerProblem(
                        fqConstructor,
                        JpaBundle.message("esprito.jpa.inspection.constructor.not.qualified"),
                        ProblemHighlightType.WARNING
                    )

                    return
                }
            }

            //не было совпадений
            holder.registerProblem(
                fqConstructor,
                JpaBundle.message("esprito.jpa.inspection.constructor.entity.not.found", fqConstructorText),
                ProblemHighlightType.WARNING
            )

        }
    }

}