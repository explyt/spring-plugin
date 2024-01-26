package com.esprito.spring.core.providers

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
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

        val hints = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getElementNameHints(module)

        val elementText = if (element is YAMLKeyValue) YAMLUtil.getConfigFullName(element) else element.text
        val targets = hints.asSequence()
            .filter { it.name == elementText }
            .map { it.jsonProperty }
            .toList()
        if (targets.isEmpty()) return

        val builder = NavigationGutterIconBuilder.create(SpringIcons.Hint)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { targets })
            .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.metadata.usage"))
            .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.metadata.usage"))
            .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.metadata.usage"))

        result += builder.createLineMarkerInfo(element)

    }
}