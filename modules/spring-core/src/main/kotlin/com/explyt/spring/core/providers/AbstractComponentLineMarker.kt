/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.core.util.SpringCoreUtil.hasComponentAnnotation
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.util.TypeConversionUtil

object AbstractComponentLineMarker {

    fun isAbstractComponent(psiClass: PsiClass): Boolean =
        !psiClass.isInterface
                && psiClass.hasModifierProperty(PsiModifier.ABSTRACT)
                && psiClass.hasComponentAnnotation()
                && !SpringCoreUtil.isCandidateComponent(psiClass)

    fun create(
        anchor: PsiElement,
        abstractClass: PsiClass,
        beans: Collection<PsiBean>,
        factories: Map<PsiMethod, PsiMethod> = emptyMap(),
        injectionPoints: () -> Collection<PsiElement>
    ): RelatedItemLineMarkerInfo<PsiElement> {
        val implementations = implementationsOf(abstractClass, beans, factories)
        return NavigationGutterIconBuilder.create(
            SpringIcons.SpringBeanDependencies,
            SpringCoreBundle.message("explyt.spring.gutter.group.bean")
        )
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy {
                StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_ABSTRACT_COMPONENT)
                (implementations.filter { it.isValid } + injectionPoints())
                    .distinct()
                    .sortedInAbstractComponentTargetOrder()
            })
            .setTooltipText(
                SpringCoreBundle.message("explyt.spring.gutter.abstract.component.tooltip", implementations.size)
            )
            .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.abstract.component.popup.title"))
            .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.abstract.component.notfound"))
            .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
            .createLineMarkerInfo(anchor)
    }

    fun implementationsOf(
        abstractClass: PsiClass,
        beans: Collection<PsiBean>,
        factories: Map<PsiMethod, PsiMethod> = emptyMap()
    ): List<PsiMember> {
        val typedBeans = beans.asSequence()
            .onEach { ProgressManager.checkCanceled() }
            .filter { it.psiClass.isValid && it.psiMember.isValid }
            .filter { it.psiMember != abstractClass && it.psiClass.isSubtypeOf(abstractClass) }
            .map { it.psiMember }
        val genericFactories = factories.asSequence()
            .onEach { ProgressManager.checkCanceled() }
            .filter { (factory, effectiveFactory) -> factory.isValid && effectiveFactory.isValid }
            .filter { (_, effectiveFactory) ->
                effectiveFactory.boundOfGenericReturnType()?.isSubtypeOf(abstractClass) == true
            }
            .map { (factory, _) -> factory }
        return (typedBeans + genericFactories).distinct().toList()
    }

    private fun PsiClass.isSubtypeOf(abstractClass: PsiClass): Boolean =
        this == abstractClass || isInheritor(abstractClass, true)

    private fun PsiMethod.boundOfGenericReturnType(): PsiClass? {
        val returnType = returnType?.takeIf { it.resolvedPsiClass is PsiTypeParameter } ?: return null
        return TypeConversionUtil.erasure(returnType).resolvedPsiClass
    }
}
