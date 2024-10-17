package com.esprito.spring.web.inspections

import com.esprito.inspection.SpringBaseLocalInspectionTool
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.web.SpringWebBundle
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.ProblemHighlightType.WARNING
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.uast.UastVisitorAdapter
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class OpenFeignInspection : SpringBaseLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(LoadBalancedVisitor(holder), true)
    }
}

private class LoadBalancedVisitor(private val holder: ProblemsHolder) : AbstractUastNonRecursiveVisitor() {
    override fun visitAnnotation(node: UAnnotation): Boolean {
        problemLoadBalanced(node)
        return super.visitAnnotation(node)
    }

    private fun problemLoadBalanced(node: UAnnotation) {
        if (node.qualifiedName != SpringCoreClasses.LOAD_BALANCED) return
        val containingClass = findClassOfAnnotatedMethod(node)?.javaPsi ?: return
        if (!containingClass.isMetaAnnotatedBy(SpringCoreClasses.OPEN_FEIGN_CLIENT)) return

        val psiElement = node.sourcePsi ?: return
        holder.registerProblem(
            psiElement,
            SpringWebBundle.message("esprito.spring.web.inspection.openfeign.client"),
            WARNING
        )
    }

    private fun findClassOfAnnotatedMethod(annotation: UAnnotation): UClass? {
        return generateSequence(annotation.uastParent) { it.uastParent }
            .filterIsInstance<UMethod>()
            .mapNotNull { it.getContainingUClass() }
            .firstOrNull()
    }

}