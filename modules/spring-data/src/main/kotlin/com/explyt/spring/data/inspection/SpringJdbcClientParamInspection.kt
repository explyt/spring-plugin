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

package com.explyt.spring.data.inspection

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.data.SpringDataBundle
import com.explyt.spring.data.SpringDataClasses
import com.explyt.spring.data.providers.findSqlExpression
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.uast.UastVisitorAdapter
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor


class SpringJdbcClientParamInspection : SpringBaseUastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return false
        val uFile = file.toUElement() as? UFile
        if (uFile != null) {
            if (uFile.imports.none { it.asRenderString().contains("org.spring") }) return false
        }
        return JavaPsiFacade.getInstance(module.project)
            .findClass(SpringDataClasses.JDBC_CLIENT, module.moduleWithLibrariesScope) != null
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(UastReferenceVisitor(holder, isOnTheFly), true)
    }
}

private class UastReferenceVisitor(
    private val holder: ProblemsHolder, private val isOnTheFly: Boolean
) : AbstractUastNonRecursiveVisitor() {

    override fun visitCallExpression(node: UCallExpression): Boolean {
        if (node.kind == UastCallKind.METHOD_CALL && node.methodName == "sql") {
            processCallExpression(node)
        }
        return true
    }

    override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression): Boolean {
        if (node.lang == KotlinLanguage.INSTANCE) return true
        val uCallExpression = (node.selector as? UCallExpression) ?: return true
        if (uCallExpression.kind == UastCallKind.METHOD_CALL && uCallExpression.methodName == "sql") {
            processCallExpression(uCallExpression)
        }
        return super.visitExpression(node)
    }

    private fun processCallExpression(uSqlCallExpression: UCallExpression) {
        val sqlString = findSqlExpression(uSqlCallExpression)?.evaluateString() ?: return
        var i = 0
        var currentNode: UElement? = uSqlCallExpression
        var isCompletedExpression = false
        val paramUCallExpressions = mutableListOf<UCallExpression>()
        while (currentNode?.uastParent != null) {
            if (i > 100) break
            val nextCallNode = currentNode.uastParent as? UCallExpression
                ?: (currentNode.uastParent as? UQualifiedReferenceExpression)?.selector as? UCallExpression
            if (isTerminalJdbcClientMethod(nextCallNode)) {
                isCompletedExpression = true
            }
            if (nextCallNode != null && isParamJdbcClientMethod(nextCallNode) && nextCallNode.valueArgumentCount > 1) {
                paramUCallExpressions.add(nextCallNode)
            }
            if (currentNode.uastParent is UBlockExpression) break
            currentNode = currentNode.uastParent
            i++
        }
        val clientCallData = JdbcClientCallData(sqlString, paramUCallExpressions, isCompletedExpression)
        analyzeJdbcClientCall(clientCallData, holder)
    }

    private fun analyzeJdbcClientCall(clientCallData: JdbcClientCallData, holder: ProblemsHolder) {
        if (clientCallData.paramUCallExpressions.isEmpty()) return
        val indexCount = clientCallData.sqlString.count { it == '?' }
        if (indexCount > 0) {
            val groupByIndex = clientCallData.paramUCallExpressions
                .map { it.getArgumentForParameter(0)?.evaluateString() to it }
                .filter { it.first != null }
                .groupBy { it.first }
            clientCallData.paramUCallExpressions.forEach {
                try {
                    val argumentForParameter = it.getArgumentForParameter(0) ?: return@forEach
                    val sourcePsi = argumentForParameter.sourcePsi ?: return@forEach
                    val paramIndex = argumentForParameter.evaluateString() ?: return@forEach
                    if (paramIndex == "0") {
                        val message = SpringDataBundle.message("explyt.spring.jdbc.client.inspection.param.zero")
                        holder.registerProblem(sourcePsi, message, ProblemHighlightType.WARNING)
                    } else if (paramIndex.toInt() > indexCount) {
                        val message = SpringDataBundle
                            .message("explyt.spring.jdbc.client.inspection.param.out.range", indexCount)
                        holder.registerProblem(sourcePsi, message, ProblemHighlightType.WARNING)
                    } else if (groupByIndex.getOrDefault(paramIndex, emptyList()).size > 1) {
                        val message = SpringDataBundle.message("explyt.spring.jdbc.client.inspection.param.duplicate")
                        holder.registerProblem(sourcePsi, message, ProblemHighlightType.WARNING)
                    }
                } catch (_: Exception) {
                }
            }
        } else {
            clientCallData.paramUCallExpressions.forEach {
                try {
                    val argumentForParameter = it.getArgumentForParameter(0) ?: return@forEach
                    val sourcePsi = argumentForParameter.sourcePsi ?: return@forEach
                    val paramName = argumentForParameter.evaluateString() ?: return@forEach
                    if (!clientCallData.sqlString.contains(":$paramName ")
                        && !clientCallData.sqlString.endsWith(":$paramName")
                    ) {
                        val message = SpringDataBundle
                            .message("explyt.spring.jdbc.client.inspection.param.not.found", paramName)
                        holder.registerProblem(sourcePsi, message, ProblemHighlightType.WARNING)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun isParamJdbcClientMethod(nextCallNode: UCallExpression?): Boolean =
        nextCallNode != null && nextCallNode.kind == UastCallKind.METHOD_CALL && nextCallNode.methodName == "param"

    private fun isTerminalJdbcClientMethod(nextCallNode: UCallExpression?): Boolean =
        nextCallNode != null && nextCallNode.kind == UastCallKind.METHOD_CALL
                && (nextCallNode.methodName == "query" || nextCallNode.methodName == "update")
}

private data class JdbcClientCallData(
    val sqlString: String,
    val paramUCallExpressions: List<UCallExpression>,
    val isCompletedExpression: Boolean
)