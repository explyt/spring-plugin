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

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.quickfix.AddAnnotationParameterKotlinFix
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.util.ExplytPsiUtil.containKotlinKeyword
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType.WARNING
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.uast.UMethod

class SpringKotlinInternalBeanInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        if (!method.containKotlinKeyword(KtTokens.INTERNAL_KEYWORD)) return emptyArray()

        method.findAnnotation(SpringCoreClasses.BEAN) ?: return emptyArray()

        val problems = ProblemsHolder(manager, method.javaPsi.containingFile, isOnTheFly)
        checkMethodAnnotations(method, problems)
        return problems.resultsArray
    }

    private fun checkMethodAnnotations(method: UMethod, problems: ProblemsHolder) {
        val jvmNameAnnotation = method.findAnnotation("kotlin.jvm.JvmName")
        val hasJvmName = jvmNameAnnotation?.attributeValues?.mapNotNull { it.evaluate() }?.any()
        if (hasJvmName == true) return

        if (hasBeanNameValue(method)) return
        val psiElement = method.uastAnchor?.sourcePsi ?: return
        val methodName = psiElement.text ?: return
        problems.registerProblem(
            psiElement, message("explyt.spring.inspection.kotlin.internal.warning"), WARNING,
            AddAnnotationParameterKotlinFix(psiElement, SpringCoreClasses.BEAN, methodName)
        )
    }

    private fun hasBeanNameValue(method: UMethod): Boolean {
        val beanAnnotation = method.findAnnotation(SpringCoreClasses.BEAN) ?: return false
        val annotationPsiElement = beanAnnotation.javaPsi ?: return false
        val module = ModuleUtilCore.findModuleForPsiElement(annotationPsiElement) ?: return false
        val metaHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.BEAN)

        return metaHolder.getAnnotationMemberValues(annotationPsiElement, setOf("name"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .any()
    }
}