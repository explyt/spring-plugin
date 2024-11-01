/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.jpa

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