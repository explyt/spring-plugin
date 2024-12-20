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

import com.explyt.base.LibraryClassCache
import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.references.contributors.webClient.EndpointResult
import com.explyt.spring.web.references.contributors.webClient.WebClientMethodCompletionContributor.WebClientMethodCompletionProvider.Companion.endpointsTypesForExpression
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.uast.UastVisitorAdapter
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class WebClientMethodWrongTypeInspection : SpringBaseLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return super.isAvailableForFile(file) && LibraryClassCache.searchForLibraryClass(
            file.project, SpringWebClasses.WEB_CLIENT_URI_SPEC
        ) != null
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(WebClientBodyTypeVisitor(holder), true)
    }

}

class WebClientBodyTypeVisitor(private val holder: ProblemsHolder) :
    AbstractUastNonRecursiveVisitor() {

    override fun visitExpression(node: UExpression): Boolean {
        val psiTypeExpression = node.javaPsi as? PsiClassObjectAccessExpression ?: return true
        val methodCall = psiTypeExpression.getUastParentOfType<UCallExpression>() ?: return true

        if (methodCall.kind != UastCallKind.METHOD_CALL) return true
        val psiMethod = methodCall.resolve() ?: return true
        if (psiMethod.containingClass?.qualifiedName != SpringWebClasses.WEB_CLIENT_RESPONSE_SPEC) return true
        val receiver: UQualifiedReferenceExpression =
            methodCall.receiver as? UQualifiedReferenceExpression ?: return true
        if (methodCall.methodName !in setOf("bodyToMono", "bodyToFlux")) return true
        val module = ModuleUtilCore.findModuleForPsiElement(psiTypeExpression) ?: return true
        val typeToCheck = psiTypeExpression.text.removeSuffix(".class")

        endpointsTypesForExpression(receiver, module)?.let { (uri, endpointsTypes) ->
            val endpointResults = endpointsTypes
                .mapNotNull {
                    EndpointResult.of(
                        it,
                        psiTypeExpression.language,
                        psiTypeExpression.language == JavaLanguage.INSTANCE
                    )
                }
                .filter { it.wrapperName != null || psiTypeExpression.language == KotlinLanguage.INSTANCE }
                .mapTo(mutableSetOf()) { it.typeReferencePresentable }

            if (endpointResults.isEmpty()) return true
            if (!endpointResults.contains(typeToCheck)) {

                holder.registerProblem(
                    psiTypeExpression,
                    SpringWebBundle.message(
                        "explyt.spring.web.inspection.webClient.type.doesnt.match",
                        uri, endpointResults.joinToString()
                    ),
                    ProblemHighlightType.WEAK_WARNING
                )

            }
        }

        return true
    }

    override fun visitTypeReferenceExpression(node: UTypeReferenceExpression): Boolean {
        val psiNode = node.sourcePsi ?: return true

        val methodCall = node.uastParent as? UCallExpression ?: return true
        if (methodCall.kind != UastCallKind.METHOD_CALL) return true
        val psiMethod = methodCall.resolve() ?: return true
        if (psiMethod.containingClass?.qualifiedName != "org.springframework.web.reactive.function.client.WebClientExtensionsKt") return true
        val receiver: UQualifiedReferenceExpression =
            methodCall.receiver as? UQualifiedReferenceExpression ?: return true
        if (methodCall.methodName !in setOf("bodyToMono", "bodyToFlux", "awaitBody")) return true
        if (receiver.getExpressionType()?.canonicalText != SpringWebClasses.WEB_CLIENT_RESPONSE_SPEC) return true

        val module = ModuleUtilCore.findModuleForPsiElement(psiNode) ?: return true
        val typeToCheck = node.type.presentableText

        endpointsTypesForExpression(receiver, module)?.let { (uri, endpointTypes) ->
            val endpointResults = endpointTypes
                .mapNotNull {
                    EndpointResult.of(
                        it,
                        psiNode.language,
                        psiNode.language == JavaLanguage.INSTANCE
                    )
                }
                .filter { it.wrapperName != null || psiNode.language == KotlinLanguage.INSTANCE }
                .mapTo(mutableSetOf()) { it.typeReferencePresentable }

            if (endpointResults.isEmpty()) return true
            if (!endpointResults.contains(typeToCheck)) {

                holder.registerProblem(
                    psiNode,
                    SpringWebBundle.message(
                        "explyt.spring.web.inspection.webClient.type.doesnt.match",
                        uri, endpointResults.joinToString()
                    ),
                    ProblemHighlightType.WEAK_WARNING
                )

            }
        }

        return true
    }

}
