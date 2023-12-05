package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.inspections.utils.ResourceFileInspectionUtil
import com.esprito.util.EspritoPsiUtil.isString
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.InheritanceUtil
import com.intellij.uast.UastVisitorAdapter
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.isInjectionHost
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class PsiFileReferenceInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(PsiFileReferenceVisitor(holder, isOnTheFly), true)
    }
}

private class PsiFileReferenceVisitor(
    private val holder: ProblemsHolder, private val isOnTheFly: Boolean
) : AbstractUastNonRecursiveVisitor() {

    override fun visitAnnotation(node: UAnnotation): Boolean {
        checkValueAnnotation(node)
        return true
    }

    override fun visitCallExpression(node: UCallExpression): Boolean {
        checkCallExpression(node)
        return true
    }

    private fun checkValueAnnotation(node: UAnnotation) {
        if (node.qualifiedName != SpringCoreClasses.VALUE) return
        val psiElement = node.findAttributeValue("value")?.sourcePsi ?: return
        val valueText = ElementManipulators.getValueText(psiElement)
        val problems = ResourceFileInspectionUtil.getPathProblems(
            PropertiesFileType.INSTANCE,
            valueText,
            psiElement,
            holder.manager,
            isOnTheFly
        )
        problems.forEach { holder.registerProblem(it) }
    }

    private fun checkCallExpression(node: UCallExpression) {
        if (node.kind != UastCallKind.METHOD_CALL) return
        val methodName = node.methodName
        val psiMethod = node.resolve() ?: return
        val targetClass = psiMethod.containingClass ?: return
        checkResourceLoaderGetResource(methodName, node, targetClass)
        checkResourceUtil(targetClass, node)

    }

    private fun checkResourceLoaderGetResource(
        methodName: String?, node: UCallExpression, targetClass: PsiClass
    ) {
        if (methodName == "getResource" && node.valueArgumentCount == 1) {
            val uArgumentExpression = node.getArgumentForParameter(0) ?: return
            if (uArgumentExpression.isInjectionHost()
                && InheritanceUtil.isInheritor(targetClass, SpringCoreClasses.RESOURCE_LOADER)
            ) {
                val psiElement = uArgumentExpression.sourcePsi ?: return
                val valueText = ElementManipulators.getValueText(psiElement)
                val problems = ResourceFileInspectionUtil.getPathProblems(
                    PropertiesFileType.INSTANCE,
                    valueText,
                    psiElement,
                    holder.manager,
                    isOnTheFly
                )
                problems.forEach { holder.registerProblem(it) }
            }
        }
    }

    private fun checkResourceUtil(targetClass: PsiClass, node: UCallExpression) {
        if (InheritanceUtil.isInheritor(targetClass, SpringCoreClasses.RESOURCE_UTILS)
            && node.valueArgumentCount == 1
        ) {
            val uArgumentExpression = node.getArgumentForParameter(0) ?: return
            if (uArgumentExpression.getExpressionType()?.isString == false) return
            if (!uArgumentExpression.isInjectionHost()) return

            val psiElement = uArgumentExpression.sourcePsi ?: return
            val valueText = ElementManipulators.getValueText(psiElement)
            val problems = ResourceFileInspectionUtil.getPathProblems(
                PropertiesFileType.INSTANCE,
                valueText,
                psiElement,
                holder.manager,
                isOnTheFly
            )
            problems.forEach { holder.registerProblem(it) }
        }
    }
}