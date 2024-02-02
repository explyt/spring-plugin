package com.esprito.spring.web.inspections

import com.esprito.spring.web.SpringWebBundle
import com.esprito.spring.web.SpringWebClasses
import com.esprito.util.EspritoPsiUtil.isAnnotatedBy
import com.intellij.codeInsight.daemon.impl.quickfix.MethodReturnTypeFix
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiTypes
import org.jetbrains.uast.UMethod

class SpringInitBinderNonVoidInspection : AbstractBaseUastLocalInspectionTool() {

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
                SpringWebBundle.message("esprito.spring.web.inspection.initBinder"),
                isOnTheFly,
                arrayOf(
                    MethodReturnTypeFix(psiMethod, PsiTypes.voidType(), true)
                ),
                ProblemHighlightType.GENERIC_ERROR
            )
        )
    }

}