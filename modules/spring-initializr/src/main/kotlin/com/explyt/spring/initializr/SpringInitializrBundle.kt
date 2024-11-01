package com.explyt.spring.initializr

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.SpringInitializrBundle"

object SpringInitializrBundle : AbstractBundle(BUNDLE) {

    @JvmStatic
    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any): String =
        getMessage(key, *params)
}