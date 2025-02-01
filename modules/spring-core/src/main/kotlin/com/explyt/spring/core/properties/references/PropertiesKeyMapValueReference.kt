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

import com.explyt.spring.core.completion.insertHandler.YamlKeyConfigurationPropertyInsertHandler
import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.explyt.spring.core.util.PropertyUtil
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.base.util.substringAfterLastOrNull

class PropertiesKeyMapValueReference(
    element: PsiElement,
    private val module: Module,
    rangeInElement: TextRange,
    private val property: ConfigurationProperty
) : PsiReferenceBase.Poly<PsiElement>(element, rangeInElement, true) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val text = element.text.substringAfterLast(".")
        if (PropertyUtil.toCommonPropertyForm(text) != PropertyUtil.toCommonPropertyForm(property.name)) return ResolveResult.EMPTY_ARRAY
        return handleMapProperty(element.project)
    }

    override fun getVariants(): Array<LookupElementBuilder> {
        return arrayOf(
            LookupElementBuilder.create(property.name)
                .withInsertHandler(YamlKeyConfigurationPropertyInsertHandler())
                .withTypeText(property.type?.substringAfterLastOrNull(".") ?: "", true)
                .withIcon(AllIcons.Nodes.Property)
        )
    }

    private fun handleMapProperty(project: Project): Array<ResolveResult> {
        val type = property.sourceType ?: return emptyArray()

        val methods = getMethodsTypeByMap(project, type, element.text)
            .filter { PropertyUtil.isNameSetMethod(it.name, element.text) }
            .ifEmpty { return emptyArray() }
        return PropertyUtil.resolveResults(methods.first())
    }

    private fun getMethodsTypeByMap(project: Project, valueType: String, prefix: String): List<PsiMember> {
        val result = hashMapOf<String, ConfigurationProperty>()
        val qualifiedName = valueType.substringBeforeLast('#').replace('$', '.')
        val foundClass =
            JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project))
                ?: return emptyList()

        PropertyUtil.collectConfigurationProperty(module, foundClass, foundClass, "", result)

        return result.asSequence()
            .filter { PropertyUtil.isSameProperty(it.key, prefix.substringAfterLast(".")) }
            .map { value -> value.value.sourceType?.let { PropertyUtil.findSourceMember(prefix, it, project) } }
            .filterNotNullTo(mutableListOf())
    }
}