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