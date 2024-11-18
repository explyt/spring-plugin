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

package com.explyt.spring.core.profile

import com.explyt.spring.core.SpringCoreClasses
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiConstantEvaluationHelper
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import kotlin.streams.asSequence

class AnnotationProfileSearcher(private val project: Project) : ProfileSearcher {

    private val javaPsiFacade by lazy {
        JavaPsiFacade.getInstance(project)
    }

    private val psiConstantEvaluationHelper: PsiConstantEvaluationHelper
        get() = javaPsiFacade.constantEvaluationHelper


    override fun searchActiveProfiles(module: Module): List<String> {
        return emptyList()
    }

    override fun searchProfiles(module: Module): List<String> {
        val profileAnnotations: Collection<PsiClass> = MetaAnnotationUtil
            .getAnnotationTypesWithChildren(
                module,
                SpringCoreClasses.PROFILE,
                false
            )

        return profileAnnotations.asSequence()
            .flatMap { metaProfileClass ->
                loadProfiles(metaProfileClass, module)
            }
            .distinct()
            .toList()
    }

    private fun maybeParseExpression(maybeExpression: String): List<String> {
        if (maybeExpression.contains(profileExpressionSymbols)) {
            return maybeExpression.split(profileExpressionSymbols).map {
                it.trim()
            }
        }

        return listOf(maybeExpression)
    }

    private fun loadProfiles(
        metaAnnotationClass: PsiClass,
        module: Module
    ): Sequence<String> {
        return AnnotatedElementsSearch.searchPsiClasses(metaAnnotationClass, module.moduleScope)
            .asSequence()
            .filter {
                !it.isAnnotationType
            }.flatMap {
                MetaAnnotationUtil.findMetaAnnotations(it, listOf(SpringCoreClasses.PROFILE))
                    .asSequence()
            }.flatMap {
                val attributeValue = it.findAttributeValue("value")
                    ?: return@flatMap sequenceOf()

                if (attributeValue is PsiArrayInitializerMemberValue) {
                    attributeValue.initializers.map(psiConstantEvaluationHelper::computeConstantExpression)
                        .asSequence()
                } else {
                    sequenceOf(psiConstantEvaluationHelper.computeConstantExpression(attributeValue))
                }

            }.filterIsInstance<String>()
            .flatMap { maybeParseExpression(it) }
    }

    companion object {
        private val profileExpressionSymbols = Regex("[|!&()]")
    }
}