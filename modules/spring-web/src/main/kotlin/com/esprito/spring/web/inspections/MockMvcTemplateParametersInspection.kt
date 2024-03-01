package com.esprito.spring.web.inspections

import com.esprito.spring.web.SpringWebBundle
import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.util.SpringWebUtil
import com.esprito.spring.web.util.SpringWebUtil.MULTIPART
import com.esprito.spring.web.util.SpringWebUtil.REQUEST
import com.esprito.spring.web.util.SpringWebUtil.REQUEST_METHODS
import com.esprito.spring.web.util.SpringWebUtil.URL_TEMPLATE
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.CommonClassNames.JAVA_LANG_STRING
import com.intellij.psi.PsiElementVisitor
import com.intellij.uast.UastVisitorAdapter
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class MockMvcTemplateParametersInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(MockMvcRequestBuilderVisitor(holder, isOnTheFly), true)
    }
}

private class MockMvcRequestBuilderVisitor(
    private val problemsHolder: ProblemsHolder, private val isOnTheFly: Boolean
) : AbstractUastNonRecursiveVisitor() {

    override fun visitCallExpression(node: UCallExpression): Boolean {
        checkCallExpression(node)
        return true
    }

    override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression): Boolean {
        if (node.lang.id == "kotlin")
            return true

        (node.selector as? UCallExpression)?.let {
            checkCallExpression(it)
        }
        return true
    }

    private fun checkCallExpression(uCallExpression: UCallExpression) {
        if (uCallExpression.kind != UastCallKind.METHOD_CALL) return
        val methodName = uCallExpression.methodName ?: return
        if (methodName !in REQUEST_METHODS + REQUEST + MULTIPART) return
        val psiMethod = uCallExpression.resolve() ?: return
        val urlTemplateIndex = psiMethod.parameterList
            .parameters
            .indexOfFirst { it.name == URL_TEMPLATE && it.type.canonicalText == JAVA_LANG_STRING }
        if (urlTemplateIndex < 0) return
        val varargSize = uCallExpression.valueArguments.size - (urlTemplateIndex + 1)

        val targetClass = psiMethod.containingClass ?: return
        if (targetClass.qualifiedName != SpringWebClasses.MOCK_MVC_REQUEST_BUILDERS) return

        val uUrlTemplate = uCallExpression.valueArguments.getOrNull(urlTemplateIndex) ?: return
        val psiUrlTemplate = uUrlTemplate.sourcePsi ?: return
        val urlTemplate = uUrlTemplate.evaluateString() ?: return

        val templateParametersCount = SpringWebUtil.NameInBracketsRx.findAll(urlTemplate).count()
        if (templateParametersCount < varargSize) {
            problemsHolder.registerProblem(
                problemsHolder.manager.createProblemDescriptor(
                    psiUrlTemplate,
                    psiUrlTemplate.getHighlightRange(),
                    SpringWebBundle.message("esprito.spring.web.inspection.mockMvc.parameters.many"),
                    ProblemHighlightType.WEAK_WARNING,
                    isOnTheFly
                )
            )
        } else if (templateParametersCount > varargSize) {
            problemsHolder.registerProblem(
                problemsHolder.manager.createProblemDescriptor(
                    psiUrlTemplate,
                    psiUrlTemplate.getHighlightRange(),
                    SpringWebBundle.message("esprito.spring.web.inspection.mockMvc.parameters.few"),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly
                )
            )
        }
    }

}