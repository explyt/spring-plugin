/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

sealed interface ConditionPropertyValue {
    object Missing : ConditionPropertyValue
    object Unresolvable : ConditionPropertyValue
    data class Known(val text: String) : ConditionPropertyValue
}

data class PropertyConditionSpec(
    val prefix: String,
    val names: List<String>,
    val havingValue: String,
    val matchIfMissing: Boolean
) {
    val keys: List<String> get() = names.map { "$prefix$it" }

    fun matches(valueOf: (String) -> ConditionPropertyValue): Boolean = keys.all { key ->
        when (val value = valueOf(key)) {
            ConditionPropertyValue.Missing -> matchIfMissing
            ConditionPropertyValue.Unresolvable -> true
            is ConditionPropertyValue.Known -> isMatch(value.text)
        }
    }

    private fun isMatch(value: String): Boolean =
        if (havingValue.isEmpty()) !FALSE.equals(value, ignoreCase = true)
        else havingValue.equals(value, ignoreCase = true)

    companion object {
        private const val FALSE = "false"

        fun of(prefix: String?, names: List<String>, havingValue: String?, matchIfMissing: Boolean) =
            PropertyConditionSpec(normalizedPrefix(prefix), names, havingValue.orEmpty(), matchIfMissing)

        private fun normalizedPrefix(prefix: String?): String {
            val trimmed = prefix?.trim().orEmpty()
            return if (trimmed.isEmpty() || trimmed.endsWith('.')) trimmed else "$trimmed."
        }
    }
}
