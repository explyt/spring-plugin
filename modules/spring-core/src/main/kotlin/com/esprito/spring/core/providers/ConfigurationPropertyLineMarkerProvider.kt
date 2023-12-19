package com.esprito.spring.core.providers

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.properties.ConfigurationPropertyDataRetriever
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

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
        if (properties.isEmpty()) return

        val builder = NavigationGutterIconBuilder.create(SpringIcons.Property)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(properties)
            .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.property.usage"))
            .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.property.usage"))
            .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.property.usage"))

        result += builder.createLineMarkerInfo(element)
    }

}