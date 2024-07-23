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
        return mapToList { transform(it) }.toTypedArray()
    }

}