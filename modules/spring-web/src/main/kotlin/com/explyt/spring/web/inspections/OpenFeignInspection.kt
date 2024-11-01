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

package com.explyt.spring.web.inspections

import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.web.SpringWebBundle
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
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
            SpringWebBundle.message("explyt.spring.web.inspection.openfeign.client"),
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