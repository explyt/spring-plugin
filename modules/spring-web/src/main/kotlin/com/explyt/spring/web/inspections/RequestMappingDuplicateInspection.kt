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
import com.explyt.util.ExplytPsiUtil.isPublic
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiMethod
import io.ktor.http.*
import org.jetbrains.kotlin.build.joinToReadableString
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class RequestMappingDuplicateInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        aClass: UClass, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val javaPsi = aClass.javaPsi
        if (!javaPsi.isMetaAnnotatedBy(SpringWebClasses.CONTROLLER)
            && !javaPsi.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)
        ) return emptyArray()
        val module = ModuleUtilCore.findModuleForPsiElement(javaPsi) ?: return emptyArray()
        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)

        val methodsByUrlMap = javaPsi.methods.asSequence()
            .filter { it.isPublic }
            .filter { it.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING) }
            .flatMap { toUrlMethodPair(requestMappingMah, it) }
            .groupBy { it.methodType + it.url }

        val holder = ProblemsHolder(manager, javaPsi.containingFile, isOnTheFly)
        registerProblems(methodsByUrlMap, holder, manager, isOnTheFly)
        return holder.resultsArray
    }

    private fun registerProblems(
        methodsByUrlMap: Map<String, List<DuplicateUrlInfo>>,
        holder: ProblemsHolder,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ) {
        methodsByUrlMap.forEach { (_, duplicates) ->
            if (duplicates.size < 2) return@forEach
            for (duplicate in duplicates) {
                val psiElement = duplicate.psiAnnotationMemberValue.toUElement()?.sourcePsi ?: continue
                holder.registerProblem(
                    manager.createProblemDescriptor(
                        psiElement, psiElement.getHighlightRange(),
                        SpringWebBundle.message(
                            "explyt.spring.web.inspection.requestMapping.duplicate.methods",
                            duplicates.filter { it.psiMethod.name != duplicate.psiMethod.name }
                                .map { it.psiMethod.name }.joinToReadableString()
                        ),
                        ProblemHighlightType.GENERIC_ERROR,
                        isOnTheFly
                    )
                )
            }
        }
    }

    private fun toUrlMethodPair(
        requestMappingMah: MetaAnnotationsHolder, psiMethod: PsiMethod
    ): List<DuplicateUrlInfo> {
        return requestMappingMah.getAnnotationMemberValues(psiMethod, TARGET_VALUE)
            .flatMap { toDuplicateUrlInfo(it, psiMethod, requestMappingMah) }
    }

    private fun toDuplicateUrlInfo(
        psiValue: PsiAnnotationMemberValue, psiMethod: PsiMethod, requestMappingMah: MetaAnnotationsHolder
    ): List<DuplicateUrlInfo> {
        val urlString = AnnotationUtil.getStringAttributeValue(psiValue) ?: return emptyList()
        val httpMethods = requestMappingMah.getHttpMethods(psiMethod)
        return httpMethods.map { DuplicateUrlInfo(Url(urlString), it, psiMethod, psiValue) }
    }

    private fun MetaAnnotationsHolder.getHttpMethods(
        psiMethod: PsiMethod
    ) = getAnnotationMemberValues(psiMethod, TARGET_METHOD)
        .mapTo(mutableSetOf()) { it.text.split('.').last() }

    companion object {
        private val TARGET_VALUE = setOf("value")
        private val TARGET_METHOD = setOf("method")
    }

    private data class DuplicateUrlInfo(
        val url: Url,
        val methodType: String,
        val psiMethod: PsiMethod,
        val psiAnnotationMemberValue: PsiAnnotationMemberValue,
    )
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