/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.nativecontext

import com.explyt.spring.ai.mcp.beans.BeanLookupService
import com.explyt.spring.ai.mcp.beans.BoundedBeanResponseWriter
import com.explyt.spring.core.externalsystem.sync.NativeContextSync
import com.explyt.spring.core.externalsystem.sync.NativeSyncOutcome
import com.explyt.spring.core.service.beans.BeanQueryException
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.annotations.McpToolHintValue.FALSE
import com.intellij.mcpserver.annotations.McpToolHintValue.TRUE
import com.intellij.mcpserver.annotations.McpToolHints
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import org.jetbrains.annotations.TestOnly
import kotlin.time.Duration.Companion.seconds

class NativeContextMcpToolset : McpToolset {

    @set:TestOnly
    var confirmation: NativeSyncConfirmation = McpServerConfirmation

    @McpTool("explyt_sync_spring_native_context", title = "Refresh the Native Context snapshot of a Spring application")
    @McpToolHints(readOnlyHint = FALSE, destructiveHint = FALSE, idempotentHint = FALSE, openWorldHint = TRUE)
    @McpDescription(
        description = "Call when a bean answer came from a NATIVE_SNAPSHOT older than the sources " +
                "(model.snapshotImportedAt), or before trusting a NONE from a native snapshot. " +
                "Refreshes one linked Native Context - the runtime bean snapshot explyt_find_spring_bean and " +
                "explyt_get_spring_beans read with source NATIVE or AUTO - and waits for it, at most timeoutSeconds. " +
                "Side effects: builds the modules, starts the application until its bean definitions are registered, " +
                "creates the run's log file, and Spring Boot docker-compose support may start containers; database " +
                "migrations do not run. Asks the user to confirm unless the IDE allows commands without asking; a " +
                "refusal answers DECLINED and nothing runs. " +
                "An untrusted project is never synced: the answer is NOT_TRUSTED. " +
                "Select the context with applicationClassName, taken from explyt_get_spring_boot_applications, or " +
                "with contextId, taken from a CONTEXT_REQUIRED answer. " +
                "Returns {status, context, previousImportedAt, importedAt, beanCount, stillRunning, error, choices}. " +
                "'status' is SYNCED (a new snapshot was imported: 'importedAt' and 'beanCount'), FAILED " +
                "('error.message' of the refresh; the previous snapshot and its import time stay), ALREADY_RUNNING " +
                "(a refresh of this " +
                "context is in progress; call again later), TIMED_OUT ('stillRunning' tells whether the refresh " +
                "goes on in the IDE), ENDED_WITHOUT_RESULT (the refresh ended without importing a new snapshot), " +
                "NOT_LINKED (no Native Context is linked to the application), CONTEXT_REQUIRED ('choices' lists " +
                "{contextId, label}; repeat with one contextId), DECLINED or NOT_TRUSTED. " +
                "'context' is {contextId, label, applicationClassName, runConfiguration, linkedPath} with a " +
                "project-relative linkedPath; 'previousImportedAt' and 'importedAt' are ISO-8601 UTC and absent " +
                "when unknown. An unknown or ambiguous application and a context of another application answer the " +
                "same error document as explyt_find_spring_bean."
    )
    suspend fun syncSpringNativeContext(
        @McpDescription(
            "Path to the root of the open project; another open project is never answered instead. " +
                    "Omit it when a single project is open; when several are, it is required"
        )
        projectPath: String? = null,
        @McpDescription("FQN of the @SpringBootApplication; required when the scope holds more than one")
        applicationClassName: String? = null,
        @McpDescription("Opaque id of a loaded native context, taken from a CONTEXT_REQUIRED answer")
        contextId: String? = null,
        @McpDescription("How long to wait for the refresh, $MIN_TIMEOUT_SECONDS..$MAX_TIMEOUT_SECONDS seconds, 180 by default")
        timeoutSeconds: Int = 180
    ): String {
        val project = try {
            BeanLookupService.resolveProject(projectPath)
        } catch (e: BeanQueryException) {
            return errorDocument(e)
        }
        val target = try {
            readAction { NativeSyncTargets(project).select(applicationClassName, contextId) }
        } catch (e: BeanQueryException) {
            return NativeSyncResponses.ofSelectionProblem(e.problem)?.let(NativeSyncResponses::write)
                ?: errorDocument(e)
        }
        val sync = project.service<NativeContextSync>()
        if (!sync.isProjectTrusted()) {
            return NativeSyncResponses.write(NativeSyncResponses.of(target, NativeSyncOutcome.NotTrusted))
        }
        if (!confirmation.confirm(project, target.confirmationText, target.command)) {
            return NativeSyncResponses.write(NativeSyncResponses.declined(target))
        }
        val timeout = timeoutSeconds.coerceIn(MIN_TIMEOUT_SECONDS, MAX_TIMEOUT_SECONDS).seconds
        val outcome = sync.sync(target.context, timeout)
        return NativeSyncResponses.write(NativeSyncResponses.of(target, outcome))
    }

    private fun errorDocument(e: BeanQueryException): String =
        BoundedBeanResponseWriter().writeError(e.problem, BoundedBeanResponseWriter.FALLBACK_BUDGET)

    companion object {
        const val MIN_TIMEOUT_SECONDS = 1
        const val MAX_TIMEOUT_SECONDS = 1800
    }
}
