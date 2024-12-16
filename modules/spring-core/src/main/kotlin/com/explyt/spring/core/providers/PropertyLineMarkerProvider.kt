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
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.json.psi.JsonProperty
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

        val targets = hints.asSequence()
            .filter { it.name == elementText || isMapKey(elementText, it.name, isYaml) }
            .map { it.jsonProperty }
            .groupingBy { it.containingFile.virtualFile.path.replace(SOURCES_SUFFIX, "") }
            .aggregate { _, acc: JsonProperty?, elem: JsonProperty, _ ->
                when {
                    acc == null -> elem
                    acc.containingFile.virtualFile.path.contains(SOURCES_SUFFIX) -> acc
                    else -> elem
                }
            }
            .values

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

        result += builder.createLineMarkerInfo(element)
    }

    private fun isMapKey(elementText: String, propertyName: String, isYaml: Boolean): Boolean {
        val name = propertyName.substringBefore(".keys")
        return elementText.startsWith(name) && if (isYaml) name == elementText else !elementText.contains("=")
    }
    companion object {
        const val SOURCES_SUFFIX = "-sources"
    }

}