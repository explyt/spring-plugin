package com.esprito.spring.data

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.SpringDataBundle"

object SpringDataBundle : AbstractBundle(BUNDLE) {
    @JvmStatic
    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any): String =
        getMessage(key, *params)
}