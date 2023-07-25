package com.esprito.spring.core

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.SpringRunConfigurationBundle"

object SpringRunConfigurationBundle : AbstractBundle(BUNDLE) {

    @JvmStatic
    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any): String =
        getMessage(key, *params)

    fun plural(key: @PropertyKey(resourceBundle = BUNDLE) String, amount: Int, vararg params: Any): String {
        return if(amount == 1) {
            message("$key.one", *params)
        }  else {
            message("$key.many", *params)
        }
    }
}