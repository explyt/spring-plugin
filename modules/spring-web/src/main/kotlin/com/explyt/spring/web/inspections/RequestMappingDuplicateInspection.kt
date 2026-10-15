/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.HandlerMethods
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy

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
