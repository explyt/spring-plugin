/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.quarkus.core.linemarker

import com.explyt.plugin.PluginIds
import com.explyt.quarkus.core.QuarkusCoreBundle.message
import com.explyt.quarkus.core.QuarkusCoreClasses
import com.explyt.quarkus.core.QuarkusCoreIcons
import com.explyt.quarkus.core.QuarkusUtil
import com.explyt.quarkus.core.service.QuarkusDelegateSearchService
import com.explyt.quarkus.core.service.QuarkusSearchService
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isNonPrivate
import com.explyt.util.ExplytPsiUtil.isPrivate
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiSubstitutor.EMPTY
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getUParentForIdentifier

class QuarkusDecoratorLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectSlowLineMarkers(
        elements: List<PsiElement?>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (PluginIds.CDI_JB.isEnabled()) return

        val element = elements.firstOrNull() ?: return
        if (!QuarkusUtil.isQuarkusProject(element)) return

        super.collectSlowLineMarkers(elements, result)
    }

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uMethod = getUParentForIdentifier(element) as? UMethod ?: return
        if (uMethod.isPrivate) return
        val project = element.project
        val uClass = uMethod.getContainingUClass() ?: return
        if (uClass.isInterface) return
        val sourceMethodPsi = uMethod.uastAnchor?.sourcePsi ?: return
        if (uClass.javaPsi.isMetaAnnotatedBy(QuarkusCoreClasses.DECORATOR.allFqns)) {
            val delegateClasses = QuarkusDelegateSearchService.Companion.getInstance(project).getDelegateClasses(uClass)
            if (isDecoratedMethod(uMethod, delegateClasses)) {
                val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.Advice)
                    .setAlignment(GutterIconRenderer.Alignment.LEFT)
                    .setTargets(NotNullLazyValue.lazy { findDecoratedMethods(uMethod, delegateClasses) })
                    .setTooltipText(message("explyt.quarkus.gutter.decorator.decorated.tooltip"))
                    .setPopupTitle(message("explyt.quarkus.gutter.decorator.decorated.popup.title"))
                    .setEmptyPopupText(message("explyt.quarkus.gutter.decorator.decorated.empty"))
                result.add(builder.createLineMarkerInfo(sourceMethodPsi))
            }
        } else {
            val classes = QuarkusDelegateSearchService.Companion.getInstance(project).allDelegatedClasses()
            if (isDecoratedMethod(uMethod, classes)) {
                val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.Advice)
                    .setAlignment(GutterIconRenderer.Alignment.LEFT)
                    .setTargets(NotNullLazyValue.lazy { findDecorator(uMethod) })
                    .setTooltipText(message("explyt.quarkus.gutter.decorator.goto.tooltip"))
                    .setPopupTitle(message("explyt.quarkus.gutter.decorator.goto.popup.title"))
                    .setEmptyPopupText(message("explyt.quarkus.gutter.decorator.decorated.empty"))
                result.add(builder.createLineMarkerInfo(sourceMethodPsi))
            }
        }
    }

    private fun isDecoratedMethod(
        uMethod: UMethod,
        delegateClasses: Set<PsiClass>
    ): Boolean = uMethod.javaPsi.findSuperMethods().any { it.containingClass in delegateClasses }


    private fun findDecoratedMethods(uMethod: UMethod, delegateClasses: Set<PsiClass>): Collection<PsiElement> {
        val psiMethod = uMethod.javaPsi
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return emptyList()
        val methods = QuarkusSearchService.getInstance(psiMethod.project).allBeanSequence(module)
            .filter { psiBean -> delegateClasses.any { psiBean.psiClass.isInheritor(it, true) } }
            .filter { psiBean -> !psiBean.psiClass.isMetaAnnotatedBy(QuarkusCoreClasses.DECORATOR.allFqns) }
            .flatMap { psiBean -> psiBean.psiClass.allMethods.filter { it.isNonPrivate && it.name == psiMethod.name } }
            .filter { it.containingClass?.isInterface != true }
            .toList()
        return methods.filter {
            it.getSignature(EMPTY) == psiMethod.getSignature(EMPTY)
        }
    }

    private fun findDecorator(uMethod: UMethod): Collection<PsiElement> {
        val psiMethod = uMethod.javaPsi
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return emptyList()
        return QuarkusSearchService.getInstance(psiMethod.project).allBeanSequence(module)
            .filter { psiBean -> psiBean.psiClass.isMetaAnnotatedBy(QuarkusCoreClasses.DECORATOR.allFqns) }
            .flatMap { psiBean -> psiBean.psiClass.allMethods.filter { it.isNonPrivate && it.name == psiMethod.name } }
            .filter { it.containingClass?.isInterface != true }
            .filter { it.getSignature(EMPTY) == psiMethod.getSignature(EMPTY) }
            .toList()
    }
}

