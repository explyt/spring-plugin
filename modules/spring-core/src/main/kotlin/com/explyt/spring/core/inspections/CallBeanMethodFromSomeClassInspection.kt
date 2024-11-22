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

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.service.SpringSearchService
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

        holder.registerProblem(sourcePsi, message("explyt.spring.inspection.method.same.class.title"), WARNING)
    }

}