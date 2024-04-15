package com.esprito.spring.core.inspections

import com.esprito.inspection.SpringBaseLocalInspectionTool
import com.esprito.spring.core.SpringCoreBundle.message
import com.esprito.spring.core.service.SpringSearchService
import com.intellij.codeInspection.ProblemHighlightType.WARNING
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.uast.UastVisitorAdapter
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class CallBeanMethodFromSomeClassInspection : SpringBaseLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(CallExpressionVisitor(holder), true)
    }
}

private class CallExpressionVisitor(private val holder: ProblemsHolder) : AbstractUastNonRecursiveVisitor() {

    override fun visitCallExpression(node: UCallExpression): Boolean {
        checkCallExpression(node)
        return true
    }

    private fun checkCallExpression(node: UCallExpression) {
        val containingUClass = node.getContainingUClass() ?: return
        val sourcePsi = node.sourcePsi ?: return
        SpringSearchService.getInstance(sourcePsi.project)
        val methodsInfo = SpringSearchService.getInstance(sourcePsi.project).getBeanMethods(containingUClass)
        if (node.receiver != null) return //check method qualifier
        if (node.kind != UastCallKind.METHOD_CALL) return
        if (!methodsInfo.beanPublicAnnotatedMethodNames.contains(node.methodName)) return
        val psiMethod = node.resolve() ?: return
        if (!methodsInfo.beanPublicAnnotatedMethods.contains(psiMethod)) return

        holder.registerProblem(sourcePsi, message("esprito.spring.inspection.method.same.class.title"), WARNING)
    }

}