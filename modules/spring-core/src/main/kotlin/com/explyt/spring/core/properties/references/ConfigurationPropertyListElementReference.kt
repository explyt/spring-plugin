/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.util.PropertyUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult

/**
 * Resolves an indexed collection key such as `ingest.s3-logs.sources[0].enabled`.
 *
 * The configuration property model registers the collection itself (`...sources`) but not the members of its
 * element type, so [com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference] cannot
 * resolve anything below the index. The element member is therefore looked up in the collection's element
 * class by the key part that follows the index.
 *
 * Soft on purpose: a key this reference cannot resolve is left to the other key references instead of being
 * reported as an unresolved reference.
 */
class ConfigurationPropertyListElementReference(
    element: PsiElement,
    private val module: Module,
    private val propertyKey: String
) : PsiReferenceBase.Poly<PsiElement>(element, null, true) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val collectionProperty = findCollectionProperty() ?: return emptyArray()

        val elementKey = propertyKey.substringAfter(INDEX_END, "")
        if (elementKey.isEmpty()) return resolveCollection(collectionProperty)

        val elementType = PropertyUtil.getCollectionElementType(collectionProperty) ?: return emptyArray()
        return PropertyUtil.getMembersOfType(module, elementType, elementKey)
            .firstOrNull { PropertyUtil.isNameSetMethod(it.name, elementKey) }
            ?.let { PropertyUtil.resolveResults(it) }
            ?: emptyArray()
    }

    /**
     * The collection is looked up by the exact key preceding the FIRST index, so a shorter parent key can never
     * win: [SpringConfigurationPropertiesSearch.getAllProperties] has no defined order, which makes a
     * `startsWith` search over all properties ambiguous for nested keys.
     */
    private fun findCollectionProperty(): ConfigurationProperty? {
        val indexStart = propertyKey.indexOf(INDEX_START)
        if (indexStart < 0) return null

        val collectionKey = propertyKey.substring(0, indexStart)
        return SpringConfigurationPropertiesSearch.getInstance(module.project)
            .findProperty(module, collectionKey)
            ?.takeIf { it.isList() || it.isArray() }
    }

    private fun resolveCollection(collectionProperty: ConfigurationProperty): Array<ResolveResult> {
        val sourceType = collectionProperty.sourceType ?: return emptyArray()
        val sourceMember = PropertyUtil
            .findSourceMember(collectionProperty.name, sourceType, element.project) ?: return emptyArray()
        return PropertyUtil.resolveResults(sourceMember)
    }

    companion object {
        const val INDEX_START = '['
        private const val INDEX_END = "]."
    }
}
