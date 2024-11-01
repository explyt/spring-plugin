package com.explyt.spring.aop

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.SpringAopBundle"

object SpringAopBundle : AbstractBundle(BUNDLE) {

    @JvmStatic
    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any): String =
        getMessage(key, *params)
}