package com.esprito.util

import org.jetbrains.kotlin.j2k.toKotlinTypesMap

object EspritoKotlinUtil {

    fun toKotlinType(javaFqName: String): String {
        return toKotlinTypesMap[javaFqName] ?: javaFqName
    }

    inline fun <T, R> Sequence<T>.mapToSet(transform: (T) -> R): Set<R> {
        return mapTo(mutableSetOf(), transform)
    }

}