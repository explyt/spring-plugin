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

package com.explyt.util

import org.jetbrains.kotlin.j2k.toKotlinTypesMap

object ExplytKotlinUtil {

    fun toKotlinType(javaFqName: String): String {
        return toKotlinTypesMap[javaFqName] ?: javaFqName
    }

    inline fun <T, R> Sequence<T>.mapToSet(transform: (T) -> R): Set<R> {
        return mapTo(mutableSetOf(), transform)
    }

    inline fun <T, R> Sequence<T>.mapToList(transform: (T) -> R): List<R> {
        return mapTo(mutableListOf(), transform)
    }

    inline fun <T, reified R> Sequence<T>.mapToArray(transform: (T) -> R): Array<R> {
        return mapToList(transform).toTypedArray()
    }

    inline fun <T> Sequence<T>.filterToSet(predicate: (T) -> Boolean): Set<T> {
        return this.filterTo(mutableSetOf(), predicate)
    }

    inline fun <T> Sequence<T>.filterToList(predicate: (T) -> Boolean): List<T> {
        return this.filterTo(mutableListOf(), predicate)
    }

    inline fun <reified T> Sequence<T>.filterToArray(predicate: (T) -> Boolean): Array<T> {
        return this.filterToList(predicate).toTypedArray()
    }

    private val toKotlinTypesMap = mapOf(
        "java.lang.Object" to "kotlin.Any",
        "java.lang.Byte" to "kotlin.Byte",
        "java.lang.Character" to "kotlin.Char",
        "java.lang.Double" to "kotlin.Double",
        "java.lang.Float" to "kotlin.Float",
        "java.lang.Integer" to "kotlin.Int",
        "java.lang.Long" to "kotlin.Long",
        "java.lang.Short" to "kotlin.Short",
        "java.lang.Boolean" to "kotlin.Boolean",
        "java.lang.Iterable" to "kotlin.collections.Iterable",
        "java.util.Iterator" to "kotlin.collections.Iterator",
        "java.util.List" to "kotlin.collections.List",
        "java.util.Collection" to "kotlin.collections.Collection",
        "java.util.Set" to "kotlin.collections.Set",
        "java.util.Map" to "kotlin.collections.Map",
        "java.util.Map.Entry" to "kotlin.collections.Map.Entry",
        "java.util.ListIterator" to "kotlin.collections.ListIterator",
    )

}