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
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.SpringProperties.COLON
import com.explyt.spring.core.properties.dataRetriever.ConfigurationPropertyDataRetrieverFactory
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import org.jetbrains.uast.getUParentForIdentifier
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import javax.swing.Icon
import com.explyt.spring.core.properties.dataRetriever.ConfigurationPropertyDataRetriever.Companion as DataRetriever

class ConfigurationPropertyLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (!SpringCoreUtil.isSpringProject(element.project)) return
        val uParent = getUParentForIdentifier(element) ?: return
        val dataRetriever = ConfigurationPropertyDataRetrieverFactory.createFor(uParent) ?: return
        val psiClass = dataRetriever.getContainingClass() ?: return
        val memberName = dataRetriever.getMemberName() ?: return
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        val nameElement = dataRetriever.getNameElementPsi() ?: return

        val prefixValue = DataRetriever.getPrefixValue(psiClass)
        if (prefixValue.isBlank()) return

        val properties = dataRetriever.getRelatedProperties(prefixValue, memberName, module)
        val hints = dataRetriever.getMetadataName(prefixValue, memberName, module)

        val targets = properties + hints
        if (targets.isEmpty()) return

        val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringSetting)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy {
                StatisticActionId.GUTTER_TARGET_PROPERTY
                targets
            })
            .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.property.usage"))
            .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.property.usage"))
            .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.property.usage"))
            .setTargetRenderer { getTargetRender() }

        result += builder.createLineMarkerInfo(nameElement)
    }

    private fun getTargetRender(): PsiTargetPresentationRenderer<PsiElement> {
        return object : PsiTargetPresentationRenderer<PsiElement>() {
            override fun getElementText(element: PsiElement): String {
                if (element is Property) {
                    return element.text
                } else if (element is YAMLKeyValue) {
                    val value = element.value
                    if (value != null) {
                        return YAMLUtil.getConfigFullName(element) + COLON + " ${value.text}"
                    }
                } else if (element is JsonStringLiteral) {
                    return "Hint for ${element.value}"
                }
                return super.getElementText(element)
            }

            override fun getIcon(element: PsiElement): Icon? {
                if (element is Property || element is YAMLKeyValue) {
                    return SpringIcons.Property
                } else if (element is JsonStringLiteral) {
                    return SpringIcons.Hint
                }
                return super.getIcon(element)
            }

            override fun getContainerText(element: PsiElement): String? {
                if (element is JsonStringLiteral) {
                    return SymbolPresentationUtil.getFilePathPresentation(element.containingFile)
                }
                return super.getContainerText(element)
            }
        }
    }

}