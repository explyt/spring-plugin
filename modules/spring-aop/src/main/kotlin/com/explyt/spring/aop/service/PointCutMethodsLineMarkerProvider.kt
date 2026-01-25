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

package com.explyt.spring.aop.service

import com.explyt.spring.aop.SpringAopBundle
import com.explyt.spring.aop.SpringAopIcons
import com.explyt.spring.core.externalsystem.model.SpringAspectData
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.util.ExplytPsiUtil.isPrivate
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElement

class PointCutMethodsLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uClass = element.toUElement() ?: return

        if (uClass is UClass) {
            val javaPsi = uClass.javaPsi
            val aspectSearchService = AspectSearchService.getInstance(element.project)
            val pointCutClasses = aspectSearchService.getPointCutQualifiedClasses()
            if (javaPsi.qualifiedName?.let { pointCutClasses.contains(it) } == false) return
            val aspectDataByMethodName = aspectSearchService.getAspectsData()
                .filter { it.beanQualifiedClassName == javaPsi.qualifiedName }
                .groupBy { it.beanMethodName }
            for (method in uClass.methods) {
                val sourcePsi = method.uastAnchor?.sourcePsi ?: continue
                if (method.isPrivate) continue
                val springAspectsListByMethod = aspectDataByMethodName[method.name] ?: continue
                val parametersCount = method.javaPsi.parameterList.parameters.size
                val parametersList = method.javaPsi.parameterList.parameters
                    .mapNotNull { toTypeQualifiedName(it.type) }
                val aspectDataFilteredByParams = springAspectsListByMethod
                    .filter { it.methodQualifiedParams.size == parametersCount }
                    .filter { it.methodQualifiedParams == parametersList }
                    .takeIf { it.isNotEmpty() } ?: continue
                val builder = NavigationGutterIconBuilder.create(SpringAopIcons.Advice)
                    .setAlignment(GutterIconRenderer.Alignment.LEFT)
                    .setTargets(NotNullLazyValue.lazy { findMethods(aspectDataFilteredByParams, element.project) })
                    .setTooltipText(SpringAopBundle.message("explyt.spring.gutter.aop.tooltip.pointcut.method"))
                    .setPopupTitle(SpringAopBundle.message("explyt.spring.gutter.aop.title.pointcut.method"))
                    .setEmptyPopupText(SpringAopBundle.message("explyt.spring.gutter.aop.title.pointcut.method.empty"))
                result.add(builder.createLineMarkerInfo(sourcePsi))
            }
        }
    }

    private fun toTypeQualifiedName(psiType: PsiType): String? {
        val qualifiedName = psiType.resolvedPsiClass?.qualifiedName
        if (qualifiedName != null) return qualifiedName
        if (TypeConversionUtil.isPrimitiveAndNotNull(psiType)) {
            return psiType.canonicalText
        }
        if (TypeConversionUtil.isPrimitiveWrapper(psiType)) {
            return psiType.canonicalText
        }
        return qualifiedName
    }

    private fun findMethods(aspects: List<SpringAspectData>, project: Project): Collection<PsiElement> {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_ASPECTJ_DECLARATION)
        return aspects.mapNotNull { toPsiMethod(it, project) }
    }

    private fun toPsiMethod(aspectData: SpringAspectData, project: Project): PsiElement? {
        return JavaPsiFacade.getInstance(project)
            .findClass(aspectData.aspectQualifiedClassName, project.allScope())
            ?.findMethodsByName(aspectData.aspectMethodName, false)?.firstOrNull()
    }
}