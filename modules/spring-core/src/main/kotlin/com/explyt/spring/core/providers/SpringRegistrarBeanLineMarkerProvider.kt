/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.beans.discoverer.SpringBeanRegistrarAdditionalBeansDiscoverer.Companion.BEAN_REGISTRAR
import com.explyt.spring.core.service.beans.discoverer.SpringBeanRegistrarAdditionalBeansDiscoverer.Companion.BEAN_REGISTRAR_DSL
import com.explyt.util.ExplytPsiUtil.isPublic
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier
import org.jetbrains.uast.toUElement

class SpringRegistrarBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uClass = getUParentForIdentifier(element) as? UClass ?: return
        val javaPsi = uClass.javaPsi
        if (!InheritanceUtil.isInheritor(javaPsi, BEAN_REGISTRAR)) return
        uClass.methods
            .filter { it.name == "register" && it.uastParameters.size == 2 }
            .forEach { addLineMarker(it, result) }
        if (javaPsi.language == KotlinLanguage.INSTANCE) {
            if (!InheritanceUtil.isInheritor(javaPsi, BEAN_REGISTRAR_DSL)) return
            uClass.methods
                .filter { it.isConstructor && it.isPublic && it.asRenderString().contains("BeanRegistrarDsl") }
                .forEach { addLineMarker(it, result) }
        }
    }

    private fun addLineMarker(
        method: UMethod,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val sourcePsi = method.uastAnchor?.sourcePsi ?: return
        val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBean)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithAutowired(method) })
            .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.autowired.candidate"))
            .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.autowired.candidate"))
            .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.autowired.candidate"))
            .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
        result.add(builder.createLineMarkerInfo(sourcePsi))
    }

    private fun findFieldsAndMethodsWithAutowired(uMethod: UMethod): Set<PsiElement> {
        val psiMethod = uMethod.javaPsi
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return emptySet()
        val searchService = SpringSearchServiceFacade.getInstance(psiMethod.project)
        val registrarBeansByUMethod = searchService.getAllActiveBeans(module)
            .filter { it.isMember() && (it.psiMember.toUElement() as? UMethod) == uMethod }
        return registrarBeansByUMethod.mapNotNull { it.psiClass.toUElement() as? UClass }
            .flatMap { searchService.findFieldsAndMethodsWithAutowired(it, null, module) }
            .toSet()
    }
}