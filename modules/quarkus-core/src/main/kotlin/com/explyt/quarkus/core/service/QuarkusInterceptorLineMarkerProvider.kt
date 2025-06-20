/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.quarkus.core.service

import com.explyt.quarkus.core.QuarkusCoreBundle.message
import com.explyt.quarkus.core.QuarkusCoreClasses
import com.explyt.quarkus.core.QuarkusCoreIcons
import com.explyt.quarkus.core.QuarkusUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedByOrSelf
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.uast.*

class QuarkusInterceptorLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectSlowLineMarkers(
        elements: List<PsiElement?>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        val element = elements.firstOrNull() ?: return
        if (!QuarkusUtil.isQuarkusProject(element)) return

        super.collectSlowLineMarkers(elements, result)
    }

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uElement = getUParentForIdentifier(element) ?: return
        if (uElement is UClass) {
            val javaPsiClass = uElement.javaPsi
            val uAnnotations = uElement.uAnnotations
            if (javaPsiClass.isMetaAnnotatedBy(QuarkusCoreClasses.INTERCEPTOR)) {
                getInterceptorBindingAnnotationQualifiedNames(uAnnotations)
                    .forEach {
                        val sourcePsi = uElement.uastAnchor?.sourcePsi ?: return@forEach
                        val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.Advice)
                            .setAlignment(GutterIconRenderer.Alignment.LEFT)
                            .setTargets(NotNullLazyValue.lazy { findInterceptorUsages(it, javaPsiClass) })
                            .setTooltipText(message("explyt.quarkus.gutter.interceptor.usages.tooltip"))
                            .setPopupTitle(message("explyt.quarkus.gutter.interceptor.usages.popup.title"))
                            .setEmptyPopupText(message("explyt.quarkus.gutter.interceptor.usages.empty"))
                        result.add(builder.createLineMarkerInfo(sourcePsi))
                    }
            } else {
                val sourcePsi = uElement.uastAnchor?.sourcePsi ?: return
                getInterceptorBindingAnnotationQualifiedNames(uAnnotations)
                    .forEach { goToDeclarationMarker(it, sourcePsi, result) }
            }
        }
        if (uElement is UMethod) {
            val sourcePsi = uElement.uastAnchor?.sourcePsi ?: return
            getInterceptorBindingAnnotationQualifiedNames(uElement.uAnnotations)
                .forEach { goToDeclarationMarker(it, sourcePsi, result) }
        }
    }

    private fun goToDeclarationMarker(
        annotation: UAnnotation,
        sourcePsi: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.Advice)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { goToInterceptor(annotation) })
            .setTooltipText(message("explyt.quarkus.gutter.interceptor.goto.tooltip"))
            .setPopupTitle(message("explyt.quarkus.gutter.interceptor.goto.popup.title"))
            .setEmptyPopupText(message("explyt.quarkus.gutter.interceptor.goto.empty"))
        result.add(builder.createLineMarkerInfo(sourcePsi))
    }

    private fun getInterceptorBindingAnnotationQualifiedNames(uAnnotations: List<UAnnotation>): Sequence<UAnnotation> =
        uAnnotations.asSequence().filter { isInterceptorBindingAnno(it) }

    private fun isInterceptorBindingAnno(uAnnotation: UAnnotation): Boolean {
        val psiAnnotation = uAnnotation.javaPsi ?: return false
        return psiAnnotation.isMetaAnnotatedByOrSelf(QuarkusCoreClasses.INTERCEPTOR_BINDING)
    }

    private fun findInterceptorUsages(
        uAnnotation: UAnnotation, currentClass: PsiClass
    ): Collection<PsiElement> {
        val searchScope = currentClass.project.projectScope()
        val annotationClass = uAnnotation.resolve() ?: return emptyList()
        return AnnotatedElementsSearch.searchPsiMembers(annotationClass, searchScope)
            .filter { !it.isMetaAnnotatedBy(QuarkusCoreClasses.INTERCEPTOR) }

    }

    private fun goToInterceptor(uAnnotation: UAnnotation): Collection<PsiElement> {
        val annotationClass = if (uAnnotation.getContainingUClass()?.isAnnotationType == true) {
            uAnnotation.getContainingUClass()?.javaPsi
        } else {
            uAnnotation.resolve()
        } ?: return emptyList()
        return AnnotatedElementsSearch.searchPsiClasses(annotationClass, annotationClass.project.allScope())
            .filter { it.isMetaAnnotatedBy(QuarkusCoreClasses.INTERCEPTOR) }
    }
}

