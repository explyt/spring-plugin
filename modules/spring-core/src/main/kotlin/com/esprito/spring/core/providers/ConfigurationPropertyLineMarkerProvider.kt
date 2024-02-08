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
import com.intellij.lang.Language
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.uast.*
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import javax.swing.Icon

class ConfigurationPropertyLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uParent = getUParentForIdentifier(element) ?: return
        val uMethod = when (uParent) {
            is UMethod -> uParent
            is UField -> getMethodFromKtField(uParent)
            else -> null
        } ?: return

        val dataRetriever = ConfigurationPropertyDataRetriever(uMethod.javaPsi)
        val psiClass = dataRetriever.getContainingClass() ?: return
        val memberName = dataRetriever.getMemberName() ?: return
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        val nameElement = uMethod.uastAnchor?.sourcePsi ?: return

        val prefixValue = ConfigurationPropertyDataRetriever.getPrefixValue(psiClass, module)
        if (prefixValue.isBlank()) return

        val properties = dataRetriever.getRelatedProperties(prefixValue, memberName, module)
        val hints = dataRetriever.getMetadataName(prefixValue, memberName, module)

        val targets = properties + hints
        if (targets.isEmpty()) return

        val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringSetting)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { targets })
            .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.property.usage"))
            .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.property.usage"))
            .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.property.usage"))
            .setTargetRenderer { getTargetRender() }

        result += builder.createLineMarkerInfo(nameElement)
    }

    private fun getMethodFromKtField(uField: UField): UMethod? {
        val psiField = uField.javaPsi as? PsiField ?: return null
        if (psiField.language != KOTLIN_LANGUAGE.value) return null

        val name = "set${StringUtil.capitalize(psiField.name)}"

        val setter = psiField.toUElement()
            ?.sourcePsi
            ?.childrenOfType<KtPropertyAccessor>()
            ?.firstOrNull { it.isSetter }

        return if (setter == null) {
            uField.getUastParentOfType<UClass>()?.methods
                ?.firstOrNull { it.name == name }
        } else null
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

    companion object {
        val KOTLIN_LANGUAGE = lazy { Language.findLanguageByID("kotlin") }
    }
}