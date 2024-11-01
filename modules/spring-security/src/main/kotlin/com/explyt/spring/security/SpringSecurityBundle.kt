package com.explyt.spring.security

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.SpringSecurityBundle"

object SpringSecurityBundle : AbstractBundle(BUNDLE) {
    @JvmStatic
    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any): String =
        getMessage(key, *params)
}