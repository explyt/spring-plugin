package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.EVENT_LISTENER
import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isPublic
import com.intellij.codeInspection.*
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.siyeh.ig.fixes.ChangeModifierFix

class SpringEventListenerInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun checkMethod(
        method: PsiMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (method.isMetaAnnotatedBy(EVENT_LISTENER)) {
            var problems = emptyArray<ProblemDescriptor>()
            val eventListenerAnnotation = method.getMetaAnnotation(EVENT_LISTENER) ?: return null
            if (!method.isPublic) {
                problems +=
                    manager.createProblemDescriptor(
                        eventListenerAnnotation,
                        SpringCoreBundle.message("esprito.spring.inspection.method.eventListener.public"),
                        ChangeModifierFix(PsiModifier.PUBLIC),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        true
                    )
            }
            if (method.parameterList.parameters.size > 1) {
                problems +=
                    manager.createProblemDescriptor(
                        eventListenerAnnotation,
                        SpringCoreBundle.message("esprito.spring.inspection.method.eventListener.parameters"),
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