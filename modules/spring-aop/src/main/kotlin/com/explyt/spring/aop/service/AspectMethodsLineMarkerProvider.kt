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
import com.explyt.spring.aop.SpringAopClasses
import com.explyt.spring.aop.SpringAopIcons
import com.explyt.spring.core.externalsystem.model.SpringAspectData
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isPrivate
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

class AspectMethodsLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uClass = element.toUElement() ?: return

        if (uClass is UClass) {
            val javaPsi = uClass.javaPsi
            if (!javaPsi.isMetaAnnotatedBy(SpringAopClasses.ASPECT)) return
            val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
            val aspectSearchService = AspectSearchService.getInstance(module.project)
            val aspectClasses = aspectSearchService.getAspectQualifiedClasses()
            if (javaPsi.qualifiedName?.let { aspectClasses.contains(it) } == false) return
            val aspectData = aspectSearchService.getAspectsData()
                .filter { it.aspectQualifiedClassName == javaPsi.qualifiedName }
            val aspectMethods = aspectData.map { it.aspectMethodName }
            for (method in uClass.methods) {
                if (method.isPrivate) continue
                if (aspectMethods.contains(method.name)) {
                    val builder = NavigationGutterIconBuilder.create(SpringAopIcons.Advice)
                        .setAlignment(GutterIconRenderer.Alignment.LEFT)
                        .setTargets(NotNullLazyValue.lazy { findMethods(aspectData, method) })
                        .setTooltipText(SpringAopBundle.message("explyt.spring.gutter.aop.tooltip.aspect.method"))
                        .setPopupTitle(SpringAopBundle.message("explyt.spring.gutter.aop.title.aspect.method"))
                        .setEmptyPopupText(SpringAopBundle.message("explyt.spring.gutter.aop.title.aspect.method.empty"))
                    result.add(builder.createLineMarkerInfo(method.javaPsi))
                }
            }
        }
    }

    private fun findMethods(aspects: List<SpringAspectData>, uMethod: UMethod): Collection<PsiElement> {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_ASPECTJ_USAGE)
        val psiMethod = uMethod.javaPsi
        return aspects.asSequence()
            .filter { it.aspectMethodName == psiMethod.name }
            .mapNotNull { toPsiMethod(it, psiMethod.project) }
            .toList()
    }

    private fun toPsiMethod(aspectData: SpringAspectData, project: Project): PsiElement? {
        val psiClass = NativeBootUtils.findProjectClass(aspectData.beanQualifiedClassName, project) ?: return null
        val psiMethods = psiClass.findMethodsByName(aspectData.beanMethodName, true)
            .takeIf { it.isNotEmpty() } ?: return null
        if (psiMethods.size == 1) return psiMethods.first()
        for (method in psiMethods) {
            val parameters = method.parameterList.parameters
            if (parameters.mapNotNull { it.type.canonicalText } == aspectData.methodQualifiedParams) {
                return method
            }
        }
        return null
    }
}