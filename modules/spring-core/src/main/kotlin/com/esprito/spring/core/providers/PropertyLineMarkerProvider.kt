package com.esprito.spring.core.providers

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.util.SpringCoreUtil
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

        if (element is LeafPsiElement && element.language == YAMLLanguage.INSTANCE) {
            val yamlKeyValue = element.parent as? YAMLKeyValue ?: return
            if (yamlKeyValue.key != element) return
            elementText = YAMLUtil.getConfigFullName(yamlKeyValue)
        }

        val hints = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getElementNameHints(module)

        val targets = hints.asSequence()
            .filter { it.name == elementText }
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
            .setTargets(NotNullLazyValue.lazy { targets })
            .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.metadata.usage"))
            .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.metadata.usage"))
            .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.metadata.usage"))

        result += builder.createLineMarkerInfo(element)
    }

    companion object {
        const val SOURCES_SUFFIX = "-sources"
    }

}