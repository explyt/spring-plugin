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

import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.explyt.spring.core.completion.properties.PropertyType
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.completion.yaml.YamlKeyConfigurationPropertyInsertHandler
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.util.ExplytKotlinUtil.mapToList
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.base.util.substringAfterLastOrNull

class YamlKeyMapValueReference(
    element: PsiElement,
    private val module: Module,
    private val propertyKey: String,
    val range: TextRange? = null
) : PsiReferenceBase.Poly<PsiElement>(element, range, false) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        val foundProperty = PropertyUtil.configurationProperty(project, module, propertyKey) ?: return emptyArray()

        if (foundProperty.propertyType == PropertyType.MAP) {
            return handleMapProperty(project, foundProperty)
        }
        return emptyArray()
    }

    override fun getVariants(): Array<Any> {
        val project = module.project
        val properties = SpringConfigurationPropertiesSearch.getInstance(project)
            .getAllProperties(module)

        return generateVariantsByMap(project, properties)
            .toTypedArray()
    }

    private fun generateVariantsByMap(project: Project, properties: List<ConfigurationProperty>): List<LookupElement> {
        val prefixes = generatePrefixes(propertyKey.substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED))
        val configurationProperty = properties.asSequence()
            .filter { property -> prefixes.any { prefix -> property.isMap() && property.name == prefix } }
            .firstOrNull() ?: return emptyList()

        val valueType = PropertyUtil.getValueClassNameInMap(configurationProperty.type) ?: return emptyList()
        val qualifiedName = valueType.substringBeforeLast('#').replace('$', '.')
        val foundClass =
            JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project))
                ?: return emptyList()

        val results = hashMapOf<String, ConfigurationProperty>()
        PropertyUtil.collectConfigurationProperty(module, foundClass, foundClass, "", results)

        return results.asSequence().mapToList {
            LookupElementBuilder.create(it.key)
                .withInsertHandler(YamlKeyConfigurationPropertyInsertHandler())
                .withTypeText(it.value.type?.substringAfterLastOrNull(".") ?: "", true)
                .withIcon(AllIcons.Nodes.Property)
        }
    }

    private fun handleMapProperty(project: Project, foundProperty: ConfigurationProperty): Array<ResolveResult> {
        if (foundProperty.name == propertyKey) {
            return emptyArray()
        }

        val valueType = PropertyUtil.getValueClassNameInMap(foundProperty.type) ?: return emptyArray()
        val propertyMapKey = propertyKey.substringAfter("${foundProperty.name}.").substringBefore(".")
        var propertyMapValue = propertyKey.substringAfter("$propertyMapKey.")
        if (propertyMapValue == propertyKey) propertyMapValue = ""

        return if (propertyMapKey.isNotEmpty() && propertyMapValue.isEmpty()) {
            val source = PropertyUtil.findSourceMember("", valueType, project) ?: return emptyArray()
            PropertyUtil.resolveResults(source)
        } else {
            val methods = PropertyUtil.getMethodsTypeByMap(module, valueType, propertyMapValue)
                .filter { PropertyUtil.isNameSetMethod(it.name, propertyMapValue) }
                .ifEmpty { return emptyArray() }
            PropertyUtil.resolveResults(methods.first())
        }
    }



    private fun generatePrefixes(key: String): List<String> {
        val parts = key.split(".")
        val prefixes = mutableListOf<String>()
        for (i in parts.indices) {
            prefixes.add(parts.take(parts.size - i).joinToString("."))
        }
        return prefixes
    }
}