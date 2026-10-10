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

    fun verdictOf(
        valueOf: (String) -> ConditionPropertyValue,
        evidence: (detail: String, reason: ConditionReason) -> ConditionEvidence
    ): ConditionVerdict {
        val unresolvable = mutableListOf<String>()
        for (key in keys) {
            when (val value = valueOf(key)) {
                ConditionPropertyValue.Missing -> if (!matchIfMissing) {
                    return ConditionVerdict.Inactive(evidence("$key is missing", ConditionReason.NOT_MATCHED))
                }

                ConditionPropertyValue.Unresolvable -> unresolvable += key
                is ConditionPropertyValue.Known -> if (!isMatch(value.text)) {
                    return ConditionVerdict.Inactive(evidence(mismatch(key, value.text), ConditionReason.NOT_MATCHED))
                }
            }
        }
        if (unresolvable.isEmpty()) return ConditionVerdict.Active
        val detail = "${unresolvable.joinToString()} cannot be resolved"
        return ConditionVerdict.Undecided(listOf(evidence(detail, ConditionReason.PROPERTY_UNRESOLVABLE)))
    }

    private fun mismatch(key: String, value: String): String =
        if (havingValue.isEmpty()) "$key=$value" else "$key=$value, expected $havingValue"

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
