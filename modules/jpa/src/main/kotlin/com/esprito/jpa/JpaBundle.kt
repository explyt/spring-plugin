package com.esprito.jpa

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

class JpaBundle : DynamicBundle(BUNDLE) {
    companion object {
        @NonNls
        private const val BUNDLE = "messages.JpaBundle"
        @JvmStatic
        private val INSTANCE: JpaBundle = JpaBundle()

        @Nls
        fun message(
            key: @PropertyKey(resourceBundle = BUNDLE) String,
            vararg params: Any
        ): String {
            return INSTANCE.messageOrNull(key, *params) ?: ""
        }

        fun messagePointer(
            key: @PropertyKey(resourceBundle = BUNDLE) String,
            vararg params: Any
        ): Supplier<String> {
            return INSTANCE.getLazyMessage(key, *params)
        }
    }
}