/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.HandlerMethods
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedByOrSelf
import com.explyt.util.ExplytPsiUtil.isPublic
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import io.ktor.http.*
import org.jetbrains.kotlin.build.joinToReadableString
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class RequestMappingDuplicateInspection : SpringWebBaseUastLocalInspectionTool() {

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
                val psiElement = duplicate.anchor ?: continue
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
        val mapping = HandlerMethods.methodMapping(psiMethod, requestMappingMah)
        val httpMethods = mapping.methods.toSet()
        return mapping.paths.flatMap { (path, anchor) ->
            val url = urlOf(path) ?: return@flatMap emptyList()
            httpMethods.map { DuplicateUrlInfo(url, it, psiMethod, anchor) }
        }
    }

    private fun urlOf(urlString: String): Url? = try {
        Url(urlString)
    } catch (_: Exception) {
        null
    }

    private data class DuplicateUrlInfo(
        val url: Url,
        val methodType: String,
        val psiMethod: PsiMethod,
        val anchor: PsiElement?,
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
        val httpMethods = HandlerMethods.methodMapping(psiMethod, requestMappingMah).methods.toSet()

        val conflictingEndpoints = psiClass.methods.asSequence()
            .filter { it != psiMethod }
            .filter { it.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING) }
            .map { it to HandlerMethods.methodMapping(it, requestMappingMah) }
            .filter { (_, mapping) -> mapping.paths.any { it.path == endpoint } }
            .filter { (_, mapping) -> mapping.methods.intersect(httpMethods).isNotEmpty() }
            .mapTo(mutableListOf()) { (method, _) ->
                method.name
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
}
