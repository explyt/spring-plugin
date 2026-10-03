/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties

import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.properties.references.BoundMemberPath
import com.explyt.spring.core.properties.references.ConfigurationPropertyListElementReference.Companion.INDEX_START
import com.explyt.spring.core.util.PropertyUtil
import com.intellij.openapi.module.Module

/**
 * The leading part of a configuration key that the configuration model declares.
 *
 * A segment is declared when the key up to it names a property or a group leading to properties, under relaxed
 * binding. Past a declared collection the model lists no members, so the segments after an index are declared when
 * [BoundMemberPath] binds them to a member of the element type, the way the binder does.
 */
object DeclaredKeyPrefix {

    fun segmentCount(module: Module, key: String): Int {
        val segments = PropertyUtil.keySegments(key)
        val names = segments.map { it.substringBefore(INDEX_START) }
        val search = SpringConfigurationPropertiesSearch.getInstance(module.project)
        val declared = (segments.size downTo 1)
            .firstOrNull { search.isDeclaredKey(module, names.take(it).joinToString(DOT)) } ?: return 0
        if (declared == segments.size || INDEX_START !in segments[declared - 1].drop(1)) return declared

        val collection = search.findProperty(module, names.take(declared).joinToString(DOT)) ?: return declared
        val elementType = PropertyUtil.getCollectionElementType(collection) ?: return declared
        val elementPath = segments.drop(declared)
        val bound = (elementPath.size downTo 1).firstOrNull {
            BoundMemberPath.resolve(module, elementType, elementPath.take(it).joinToString(DOT)) != null
        } ?: 0
        return declared + bound
    }

    private const val DOT = "."
}
