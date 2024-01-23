package com.esprito.spring.core.providers

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.SpringProperties.COLON
import com.esprito.spring.core.properties.ConfigurationPropertyDataRetriever
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import javax.swing.Icon

class ConfigurationPropertyLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is PsiIdentifier) return

        val psiMethod = element.parent as? PsiMethod ?: return
        val dataRetriever = ConfigurationPropertyDataRetriever(psiMethod)
        val psiClass = dataRetriever.getContainingClass() ?: return
        val memberName = dataRetriever.getMemberName() ?: return
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return

        val prefixValue = ConfigurationPropertyDataRetriever.getPrefixValue(psiClass, module)
        if (prefixValue.isBlank()) return

        val properties = dataRetriever.getRelatedProperties(prefixValue, memberName, module)
        val hints = dataRetriever.getMetadataName(prefixValue, memberName, module)

        val targets = properties + hints
        if (targets.isEmpty()) return

        val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringSetting)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(targets)
            .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.property.usage"))
            .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.property.usage"))
            .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.property.usage"))
            .setTargetRenderer { getTargetRender() }

        result += builder.createLineMarkerInfo(element)
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
                    return SpringIcons.SpringSetting
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