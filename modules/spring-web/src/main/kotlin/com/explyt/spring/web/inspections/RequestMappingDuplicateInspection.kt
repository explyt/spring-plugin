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

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedByOrSelf
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.uast.UastVisitorAdapter
import org.jetbrains.kotlin.build.joinToReadableString
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class RequestMappingDuplicateInspection : SpringBaseUastLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(RequestMappingVisitor(holder, isOnTheFly), true)
    }

}

private class RequestMappingVisitor(
    private val problemsHolder: ProblemsHolder, private val isOnTheFly: Boolean
) : AbstractUastNonRecursiveVisitor() {

    override fun visitLiteralExpression(node: ULiteralExpression): Boolean {
        val psiExpression = node.sourcePsi ?: return true
        val module = ModuleUtilCore.findModuleForPsiElement(psiExpression) ?: return true
        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        val endpoint = node.evaluateString() ?: return true
        val uAnnotation = node.getParentOfType<UAnnotation>() ?: return true
        val psiAnnotation = uAnnotation.javaPsi ?: return true
        if (!psiAnnotation.isMetaAnnotatedByOrSelf(SpringWebClasses.REQUEST_MAPPING)) return true
        if (psiAnnotation.isMetaAnnotatedByOrSelf(SpringWebClasses.CONTROLLER)) return true

        val psiMethod = uAnnotation.getContainingUMethod()?.javaPsi ?: return true
        val psiClass = psiMethod.containingClass ?: return true
        if (!psiClass.isMetaAnnotatedBy(SpringWebClasses.CONTROLLER)) return true
        val httpMethods = requestMappingMah.getHttpMethods(psiMethod)

        val conflictingEndpoints = psiClass.methods.asSequence()
            .filter { it != psiMethod }
            .filter { it.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING) }
            .filter {
                requestMappingMah.getAnnotationMemberValues(it, TARGET_VALUE).any { anotherEndpoint ->
                    AnnotationUtil.getStringAttributeValue(anotherEndpoint) == endpoint
                }
            }
            .filter {
                requestMappingMah.getHttpMethods(it)
                    .intersect(httpMethods).isNotEmpty()
            }.mapTo(mutableListOf()) {
                it.name
            }

        if (conflictingEndpoints.isEmpty()) return true

        problemsHolder.registerProblem(
            problemsHolder.manager.createProblemDescriptor(
                psiExpression,
                psiExpression.getHighlightRange(),
                SpringWebBundle.message(
                    "explyt.spring.web.inspection.requestMapping.duplicate.methods",
                    conflictingEndpoints.joinToReadableString()
                ),
                ProblemHighlightType.GENERIC_ERROR,
                isOnTheFly

            )
        )

        return true
    }

    private fun MetaAnnotationsHolder.getHttpMethods(
        psiMethod: PsiMethod
    ) = getAnnotationMemberValues(psiMethod, TARGET_METHOD)
        .mapTo(mutableSetOf()) { it.text.split('.').last() }

    companion object {
        private val TARGET_VALUE = setOf("value")
        private val TARGET_METHOD = setOf("method")
    }

}