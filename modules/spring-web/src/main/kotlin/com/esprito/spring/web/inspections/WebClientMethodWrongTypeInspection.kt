package com.esprito.spring.web.inspections

import com.esprito.base.LibraryClassCache
import com.esprito.inspection.SpringBaseLocalInspectionTool
import com.esprito.spring.web.SpringWebBundle
import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.references.contributors.WebClientMethodCompletionContributor
import com.esprito.spring.web.references.contributors.WebClientMethodCompletionContributor.WebClientMethodCompletionProvider.Companion.endpointsTypesForExpression
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.uast.UastVisitorAdapter
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
                .mapNotNull { WebClientMethodCompletionContributor.EndpointResult.of(it, psiTypeExpression.language) }
                .mapTo(mutableSetOf()) { it.typeReferencePresentable }

            if (endpointResults.isEmpty()) return true
            if (!endpointResults.contains(typeToCheck)) {

                holder.registerProblem(
                    psiTypeExpression,
                    SpringWebBundle.message(
                        "esprito.spring.web.inspection.webClient.type.doesnt.match",
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
                .mapNotNull { WebClientMethodCompletionContributor.EndpointResult.of(it, psiNode.language) }
                .mapTo(mutableSetOf()) { it.typeReferencePresentable }

            if (endpointResults.isEmpty()) return true
            if (!endpointResults.contains(typeToCheck)) {

                holder.registerProblem(
                    psiNode,
                    SpringWebBundle.message(
                        "esprito.spring.web.inspection.webClient.type.doesnt.match",
                        uri, endpointResults.joinToString()
                    ),
                    ProblemHighlightType.WEAK_WARNING
                )

            }
        }

        return true
    }

}
