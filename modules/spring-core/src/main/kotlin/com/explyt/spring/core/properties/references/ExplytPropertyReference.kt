/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.util.PropertyUtil.propertyKeyPsiElement
import com.explyt.spring.core.util.PropertyUtil.toCommonPropertyForm
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.lang.properties.IProperty
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtPsiFactory

class ExplytPropertyReference(
    element: PsiElement,
    private val propertyKey: String,
    rangeInElement: TextRange,
    private val propertyPlaceholder: Boolean = false
) : PsiReferenceBase.Poly<PsiElement>(element, rangeInElement, false), HighlightedReference {

    fun getTextAttributesKey(): TextAttributesKey {
        return DefaultLanguageHighlighterColors.KEYWORD
    }

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val propertiesMap = DefinedConfigurationPropertiesSearch.getInstance(module.project)
            .getPropertiesCommonKeyMap(module)

        return propertiesMap.getOrDefault(toCommonPropertyForm(propertyKey), emptyList()).asSequence()
            .mapNotNull { getResolvedElementWithOriginalText(it) }
            .map { PsiElementResolveResult(it) }
            .toList()
            .toTypedArray()
    }

    private fun getResolvedElementWithOriginalText(definedProperty: DefinedConfigurationProperty): PsiElement? {
        val element = definedProperty.psiElement ?: return null
        val propertyElement = element.propertyKeyPsiElement() ?: element
        propertyElement.putUserData(PROPERTY_REFERENCE_ORIGINAL_TEXT, propertyKey)
        return element
    }

    override fun getVariants(): Array<Any> {
        val project: Project = myElement.project
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val allProperties = DefinedConfigurationPropertiesSearch.getInstance(project).getAllProperties(module)

        if (propertyPlaceholder) {
            return getPropertyPlaceholderVariants(allProperties).toTypedArray()
        }

        return allProperties
            .map { property ->
                val psiElement = property.psiElement
                LookupElementBuilder.create(property.key)
                    .withIcon(AllIcons.Nodes.Property)
                    .withTypeText(psiElement?.containingFile?.name)
            }.toTypedArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val references = element.references.asSequence()
            .filterIsInstance<ExplytPropertyReference>()
            .flatMap { ref ->
                ref.multiResolve(false).asSequence().mapNotNull { it.element }
            }.toList()

        if (references.isEmpty()) {
            return super.handleElementRename(newElementName)
        }

        val oldKey = propertyKey.substringAfterLast('.')
        val newText = if (newElementName.contains(".")) newElementName
        else element.text.replace(oldKey, newElementName)

        if (newElementName.contains(".")) {
            return super.handleElementRename(newElementName)
        }

        val newElement = if (element.language == KotlinLanguage.INSTANCE) {
            val factory = KtPsiFactory(element.project)
            factory.createExpression(newText)
        } else {
            PsiElementFactory.getInstance(element.project)
                .createExpressionFromText(newText, element.context)
        }
        if (element.text == newElement.text) return super.handleElementRename(newElementName)
        return element.replace(newElement)
    }

    private fun getPropertyPlaceholderVariants(allProperties: List<DefinedConfigurationProperty>) =
        allProperties.asSequence()
            .filter { it.psiElement is IProperty }
            .map {
                LookupElementBuilder.create(it.key)
                    .withTypeText(it.psiElement!!.containingFile.name, SpringIcons.PropertyKey, true)
            }
            .toList()

    companion object {
        val PROPERTY_REFERENCE_ORIGINAL_TEXT: Key<String> = Key("PropertyReferenceOriginalText")
    }
}