/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.nativecontext

import com.explyt.spring.core.externalsystem.sync.NativeSyncOutcome
import com.explyt.spring.core.service.beans.BeanContextSelector
import com.explyt.spring.core.service.beans.BeanQueryProblem
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant

enum class NativeSyncStatus {
    SYNCED, FAILED, ALREADY_RUNNING, TIMED_OUT, ENDED_WITHOUT_RESULT, NOT_LINKED, CONTEXT_REQUIRED, DECLINED, NOT_TRUSTED
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NativeSyncResponse(
    val status: NativeSyncStatus,
    val context: NativeSyncContextJson? = null,
    val previousImportedAt: String? = null,
    val importedAt: String? = null,
    val beanCount: Int? = null,
    val stillRunning: Boolean? = null,
    val error: NativeSyncErrorJson? = null,
    val choices: List<Any?>? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NativeSyncContextJson(
    val contextId: String,
    val label: String,
    val applicationClassName: String?,
    val runConfiguration: String?,
    val linkedPath: String,
)

data class NativeSyncErrorJson(val message: String)

internal object NativeSyncResponses {

    private val mapper = ObjectMapper()

    fun write(response: NativeSyncResponse): String = mapper.writeValueAsString(response)

    fun ofSelectionProblem(problem: BeanQueryProblem): NativeSyncResponse? = when (problem.code) {
        BeanContextSelector.NATIVE_CONTEXT_NOT_AVAILABLE -> NativeSyncResponse(NativeSyncStatus.NOT_LINKED)
        BeanContextSelector.CONTEXT_REQUIRED ->
            NativeSyncResponse(NativeSyncStatus.CONTEXT_REQUIRED, choices = problem.choices)
        else -> null
    }

    fun declined(target: NativeSyncTarget) = NativeSyncResponse(
        NativeSyncStatus.DECLINED, target.json, previousImportedAt = target.context.importedAt.iso()
    )

    fun of(target: NativeSyncTarget, outcome: NativeSyncOutcome): NativeSyncResponse = when (outcome) {
        is NativeSyncOutcome.Synced -> NativeSyncResponse(
            NativeSyncStatus.SYNCED, target.json,
            previousImportedAt = outcome.previousImportedAt.iso(),
            importedAt = outcome.importedAt.iso(),
            beanCount = outcome.beanCount
        )

        is NativeSyncOutcome.Failed -> NativeSyncResponse(
            NativeSyncStatus.FAILED, target.json,
            previousImportedAt = outcome.previousImportedAt.iso(),
            error = NativeSyncErrorJson(outcome.message)
        )

        NativeSyncOutcome.AlreadyRunning -> NativeSyncResponse(
            NativeSyncStatus.ALREADY_RUNNING, target.json, previousImportedAt = target.context.importedAt.iso()
        )

        is NativeSyncOutcome.TimedOut -> NativeSyncResponse(
            NativeSyncStatus.TIMED_OUT, target.json,
            previousImportedAt = outcome.previousImportedAt.iso(),
            stillRunning = outcome.stillRunning
        )

        is NativeSyncOutcome.EndedWithoutResult -> NativeSyncResponse(
            NativeSyncStatus.ENDED_WITHOUT_RESULT, target.json, previousImportedAt = outcome.previousImportedAt.iso()
        )

        NativeSyncOutcome.NotTrusted -> NativeSyncResponse(
            NativeSyncStatus.NOT_TRUSTED, target.json, previousImportedAt = target.context.importedAt.iso()
        )
    }

    private val NativeSyncTarget.json
        get() = NativeSyncContextJson(
            contextId = context.id,
            label = context.label,
            applicationClassName = context.applicationClassName,
            runConfiguration = runConfiguration,
            linkedPath = linkedPath
        )

    private fun Long?.iso(): String? = this?.let { Instant.ofEpochMilli(it).toString() }
}
