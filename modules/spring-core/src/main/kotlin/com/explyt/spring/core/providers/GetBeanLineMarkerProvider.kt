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

package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.statistic.StatisticActionId.GUTTER_BEAN_FACTORY_GET_BEAN
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.util.ExplytPsiUtil.isEqualOrInheritor
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.uast.*

class GetBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        psiElement: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uElement = psiElement.toUElement() ?: return
        if (uElement !is UIdentifier) return
        if (uElement.name != "getBean") return
        val uParent = uElement.getParentOfType<UQualifiedReferenceExpression>() ?: return
        val uCallExpression = uParent.selector as? UCallExpression ?: return
        if (uCallExpression.kind != UastCallKind.METHOD_CALL) return
        if (uCallExpression.methodName != "getBean") return
        val psiMethod = uCallExpression.resolve() ?: return
        val targetClass = psiMethod.containingClass ?: return
        if (!targetClass.isEqualOrInheritor(SpringCoreClasses.BEAN_FACTORY)) return

        val nameIndex = psiMethod.parameterList.parameters.indexOfFirst { it.name == "name" }
        val name = uCallExpression.valueArguments.getOrNull(nameIndex)?.evaluateString() ?: ""
        val requiredTypeIndex = psiMethod.parameterList.parameters.indexOfFirst { it.name == "requiredType" }
        val requiredType = when (val arg = uCallExpression.valueArguments.getOrNull(requiredTypeIndex)) {
            is UQualifiedReferenceExpression -> (arg.receiver as? UClassLiteralExpression)?.type
            is UClassLiteralExpression -> arg.type
            else -> null
        } ?: psiElement.getUastParentOfType<UBinaryExpressionWithType>()?.type

        val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { getBeans(psiElement, name, requiredType) })
            .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.bean.candidate"))
            .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.bean.candidate"))
            .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.bean.candidate"))

        result.add(builder.createLineMarkerInfo(psiElement))
    }


    private fun getBeans(psiElement: PsiElement, name: String, requiredType: PsiType?): Collection<PsiElement> {
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return emptyList()
        StatisticService.getInstance().addActionUsage(GUTTER_BEAN_FACTORY_GET_BEAN)

        return SpringSearchServiceFacade.getInstance(psiElement.project)
            .findActiveBeanDeclarations(module, name, psiElement.language, requiredType)
    }

}