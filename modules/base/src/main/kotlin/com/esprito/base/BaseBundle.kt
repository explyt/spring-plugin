package com.esprito.base

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.BaseBundle"

object BaseBundle : AbstractBundle(BUNDLE) {

    @JvmStatic
    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any): String =
        getMessage(key, *params)
}