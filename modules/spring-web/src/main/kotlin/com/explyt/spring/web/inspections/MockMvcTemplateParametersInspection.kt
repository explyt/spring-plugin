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
import com.explyt.plugin.PluginIds
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.REQUEST_METHODS
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.uast.UastVisitorAdapter
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class MockMvcTemplateParametersInspection : SpringBaseLocalInspectionTool() {
    override fun isAvailableForFile(file: PsiFile): Boolean {
        return super.isAvailableForFile(file) && PluginIds.SPRING_WEB_JB.isNotEnabled()
    }

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
        if (methodName !in REQUEST_METHODS) return
        val psiMethod = uCallExpression.resolve() ?: return
        val urlTemplateIndex = SpringWebUtil.getUrlTemplateIndex(psiMethod)
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
                    SpringWebBundle.message("explyt.spring.web.inspection.mockMvc.parameters.many"),
                    ProblemHighlightType.WEAK_WARNING,
                    isOnTheFly
                )
            )
        } else if (templateParametersCount > varargSize) {
            problemsHolder.registerProblem(
                problemsHolder.manager.createProblemDescriptor(
                    psiUrlTemplate,
                    psiUrlTemplate.getHighlightRange(),
                    SpringWebBundle.message("explyt.spring.web.inspection.mockMvc.parameters.few"),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly
                )
            )
        }
    }

}