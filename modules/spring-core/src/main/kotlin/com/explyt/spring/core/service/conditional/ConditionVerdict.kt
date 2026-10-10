/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember

sealed interface ConditionVerdict {
    object Active : ConditionVerdict
    data class Inactive(val condition: ConditionEvidence) : ConditionVerdict
    data class Undecided(val conditions: List<ConditionEvidence>) : ConditionVerdict
}

data class ConditionEvidence(
    val annotationFqn: String,
    val carrierFqn: String,
    val detail: String?,
    val reason: ConditionReason,
    val assumptions: Set<ConditionAssumption> = emptySet()
)

enum class ConditionReason {
    NOT_MATCHED,
    PROPERTY_UNRESOLVABLE,
    PROFILE_NOT_DECIDABLE,
    UNSUPPORTED_CONDITION
}

enum class ConditionAssumption {
    STATIC_BEAN_MODEL_COMPLETE,
    COMPILE_CLASSPATH_IS_RUNTIME
}

fun Sequence<ConditionVerdict>.combined(): ConditionVerdict {
    val undecided = mutableListOf<ConditionEvidence>()
    for (verdict in this) {
        when (verdict) {
            is ConditionVerdict.Inactive -> return verdict
            is ConditionVerdict.Undecided -> undecided += verdict.conditions
            ConditionVerdict.Active -> Unit
        }
    }
    return if (undecided.isEmpty()) ConditionVerdict.Active else ConditionVerdict.Undecided(undecided)
}


val PsiMember.conditionCarrierFqn: String
    get() = if (this is PsiClass) qualifiedName ?: name.orEmpty()
    else "${containingClass?.qualifiedName}#$name"
