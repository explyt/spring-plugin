/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.completion.properties.MetadataDeclarations
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

class PropertyLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (!SpringCoreUtil.isConfigurationPropertyFile(element.containingFile)) {
            return
        }
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        var elementText = element.text

        val isYaml = element.language == YAMLLanguage.INSTANCE
        if (element is LeafPsiElement && isYaml) {
            val yamlKeyValue = element.parent as? YAMLKeyValue ?: return
            if (yamlKeyValue.key != element) return
            elementText = YAMLUtil.getConfigFullName(yamlKeyValue)
        }

        val hints = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getElementNameHints(module)

        val matching = hints.filter { it.name == elementText || isMapKey(elementText, it.name, isYaml) }
        // One artifact ships the same hint in both its generated and its hand-written metadata file, and again in its
        // sources jar; they are one declaration, so only the preferred copy of each becomes a target.
        val targets = MetadataDeclarations
            .distinct(matching, { it.name }, { it.jsonProperty.containingFile })
            .map { it.jsonProperty }

        if (targets.isEmpty()) return

        val builder = NavigationGutterIconBuilder.create(SpringIcons.Hint)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy {
                StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_TARGET_ADDITIONAL_METADATA)
                targets
            })
            .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.metadata.usage"))
            .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.metadata.usage"))
            .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.metadata.usage"))

        val lineMarkerInfo = builder.createLineMarkerInfo(element)
        if (!result.contains(lineMarkerInfo)) {
            result += lineMarkerInfo
        }
    }

    private fun isMapKey(elementText: String, propertyName: String, isYaml: Boolean): Boolean {
        val name = propertyName.substringBefore(".keys")
        return elementText.startsWith(name) && if (isYaml) name == elementText else !elementText.contains("=")
    }
}
