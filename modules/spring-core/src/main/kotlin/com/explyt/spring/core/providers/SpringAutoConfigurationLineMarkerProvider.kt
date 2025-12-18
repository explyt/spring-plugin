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

package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.lang.properties.IProperty
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.UClass
import org.jetbrains.uast.getUParentForIdentifier

class SpringAutoConfigurationLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        val element = elements.firstOrNull() ?: return
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        if (!SpringCoreUtil.isSpringBootProject(module)) return

        super.collectSlowLineMarkers(elements, result)
    }

    override fun collectNavigationMarkers(
        element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uClass = getUParentForIdentifier(element) as? UClass ?: return
        val qualifiedName = uClass.qualifiedName ?: return
        val sourcePsi = uClass.uastAnchor?.sourcePsi ?: return
        if (!uClass.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)) return
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        val autoconfigureIPropertiesByStringKey = getAutoconfigureClassesCache(module)
        if (autoconfigureIPropertiesByStringKey.any { it.first.contains(qualifiedName) }) {
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringFactories)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { findFactoriesMetadataFiles(qualifiedName, module) })
                .setTooltipText(SpringCoreBundle.message("explyt.spring.factories.gutter.tooltip"))
                .setPopupTitle(SpringCoreBundle.message("explyt.spring.factories.gutter.popup.title"))
            result.add(builder.createLineMarkerInfo(sourcePsi))
        }
    }

    private fun getAutoconfigureClassesCache(module: Module): List<Pair<String, IProperty>> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                getAutoconfigureClasses(module),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getAutoconfigureClasses(module: Module): List<Pair<String, IProperty>> {
        return SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getAllFactoriesMetadataFiles(module).asSequence()
            .filter { it.key != null && it.value != null }
            .map { (it.key ?: "") + (it.value ?: "") to it }
            .toList()
    }

    private fun findFactoriesMetadataFiles(qualifiedName: String, module: Module): Collection<PsiElement> {
        val autoConfigureProperties = getAutoconfigureClassesCache(module)
        return autoConfigureProperties.asSequence()
            .filter { it.first.contains(qualifiedName) }
            .map { it.second.psiElement }
            .toList()
    }
}