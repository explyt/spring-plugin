/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import java.nio.file.InvalidPathException
import java.nio.file.Path

/** Which open project answers a tool call. */
sealed interface McpProjectChoice<out T> {

    data class Resolved<T>(val project: T) : McpProjectChoice<T>

    data class NotFound(val message: String) : McpProjectChoice<Nothing>

    data class Ambiguous(val message: String) : McpProjectChoice<Nothing>
}

/**
 * The one rule every MCP tool uses to decide which project it is being asked about.
 *
 * A supplied path is matched strictly: a caller who mistypes it, or names a project that is not open, would
 * otherwise receive a confident answer about a different codebase that reads exactly like a correct one. An
 * absent path has no wrong answer to substitute, so the single open project answers it; with several open the
 * caller is asked to name one rather than being given whichever came first.
 */
object McpProjectResolver {

    fun resolve(projectPath: String?): McpProjectChoice<Project> =
        choose(projectPath, openProjects()) { it.basePath }

    /**
     * The rule itself, over the candidates it applies to.
     *
     * Taking the list as a parameter is what makes the several-projects branch testable: a light fixture opens
     * exactly one project, so a rule reaching straight into [ProjectManager] could only be asserted for one.
     */
    fun <T> choose(projectPath: String?, candidates: List<T>, basePathOf: (T) -> String?): McpProjectChoice<T> {
        if (candidates.isEmpty()) return McpProjectChoice.NotFound("No project is open.")

        val requested = projectPath?.takeIf { it.isNotBlank() }
            ?: return single(candidates, basePathOf)

        val wanted = normalize(requested)
            ?: return McpProjectChoice.NotFound("'$requested' is not a valid path.")
        return candidates.firstOrNull { normalize(basePathOf(it)) == wanted }
            ?.let { McpProjectChoice.Resolved(it) }
            ?: McpProjectChoice.NotFound("No open project at '$requested'.")
    }

    fun normalize(path: String?): String? = try {
        path?.let { Path.of(it).normalize().toString().trimEnd('/') }
    } catch (_: InvalidPathException) {
        null
    }

    private fun <T> single(candidates: List<T>, basePathOf: (T) -> String?): McpProjectChoice<T> {
        val only = candidates.singleOrNull() ?: return McpProjectChoice.Ambiguous(
            "Several projects are open; pass projectPath to name one of " +
                    candidates.mapNotNull(basePathOf).joinToString(", ") { "'$it'" } + "."
        )
        return McpProjectChoice.Resolved(only)
    }

    private fun openProjects(): List<Project> =
        ProjectManager.getInstance().openProjects.filter { !it.isDefault }
}
