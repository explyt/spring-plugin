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

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.properties.providers.ConfigKeyPsiElement
import com.explyt.spring.core.util.PropertyUtil
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

class ExplytLibraryPropertyReference(
    element: PsiElement,
    private val propertyKey: String,
    rangeInElement: TextRange,
    private val prefix: String = ""
) : PsiReferenceBase<PsiElement>(element, rangeInElement), PsiPolyVariantReference, HighlightedReference {

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val result = mutableListOf<ResolveResult>()
        val fullPropertyKey = prefix + propertyKey

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val project = module.project
        val configurationPropertiesSearch = DefinedConfigurationPropertiesSearch.getInstance(element.project)
        val foundProps = configurationPropertiesSearch.findProperties(module, fullPropertyKey)

        foundProps.mapNotNullTo(result) { property ->
            property.psiElement?.let { PsiElementResolveResult(it) }
        }

        if (foundProps.isEmpty()) {
            val foundProperty = SpringConfigurationPropertiesSearch.getInstance(project)
                .findProperty(module, fullPropertyKey) ?: return emptyArray()
            val sourceType = foundProperty.sourceType ?: return emptyArray()
            val sourceMember = PropertyUtil.findSourceMember(fullPropertyKey, sourceType, project)
            if (sourceMember != null) {
                result += PsiElementResolveResult(ConfigKeyPsiElement(sourceMember))
            }
        }

        return result.toTypedArray()
    }

    override fun resolve(): PsiElement? {
        val resolveResults: Array<out ResolveResult> = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun getVariants(): Array<Any> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val allProperties = findPropertiesWithPrefix(prefix, module)
        return allProperties
            .map { lookupElementFor(it) }
            .toTypedArray()
    }

    private fun lookupElementFor(propertyInfo: PropertyInfo): LookupElement {
        val priority = if (propertyInfo.filename != null) Double.MAX_VALUE else 0.0

        return PrioritizedLookupElement
            .withPriority(
                LookupElementBuilder.create(propertyInfo.name.substring(prefix.length))
                    .withIcon(AllIcons.Nodes.Property)
                    .withTypeText(propertyInfo.filename),
                priority
            )
    }

    private fun findPropertiesWithPrefix(prefix: String, module: Module): Collection<PropertyInfo> {
        val project = module.project
        val result = mutableSetOf<PropertyInfo>()


        DefinedConfigurationPropertiesSearch.getInstance(project)
            .getAllProperties(module).asSequence()
            .filter { it.key.startsWith(prefix) }
            .mapTo(result) { PropertyInfo(it.key, it.psiElement) }

        SpringConfigurationPropertiesSearch.getInstance(project)
            .getAllProperties(module).asSequence()
            .filter { it.name.startsWith(prefix) }
            .map { PropertyInfo(it.name) }
            .filter { !result.contains(it) }
            .toCollection(result)

        return result
    }

    data class PropertyInfo(val name: String) {
        var filename: String? = null

        constructor(name: String, psiElement: PsiElement?) : this(name) {
            filename = psiElement?.containingFile?.name
        }
    }

}