package com.esprito.util

import org.jetbrains.kotlin.j2k.toKotlinTypesMap

object EspritoKotlinUtil {

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

}