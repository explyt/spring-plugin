/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.core.service.beans.BeanQueryException
import com.explyt.spring.core.service.beans.BeanQueryProblem
import com.explyt.spring.core.service.beans.BeanSourcePreference

/**
 * One question put to the bean tool, checked before any PSI is read.
 *
 * The two modes are kept apart deliberately: a request naming both a selector and an injection point asks two
 * different questions, and answering either one would look equally correct to a caller who meant the other.
 * Paging arguments are absent on purpose - the writer owns their bounds, and a second definition here would
 * drift from it.
 */
data class BeanLookupRequest(
    val projectPath: String,
    val applicationClassName: String? = null,
    val source: String = "AUTO",
    val contextId: String? = null,
    val typeFqn: String? = null,
    val beanName: String? = null,
    val filePath: String? = null,
    val line: Int? = null,
    val column: Int? = null,
    val includeDetails: Boolean = false
) {

    fun validate() {
        rejectBlank(projectPath, "projectPath")
        rejectBlank(applicationClassName, "applicationClassName")
        rejectBlank(contextId, "contextId")
        rejectBlank(typeFqn, "typeFqn")
        rejectBlank(beanName, "beanName")
        rejectBlank(filePath, "filePath")

        val hasSelector = typeFqn != null || beanName != null
        val hasInjection = filePath != null || line != null
        if (hasSelector && hasInjection) {
            invalid("A bean selector and an injection point are different questions; supply one of them.")
        }
        if (!hasSelector && !hasInjection) {
            invalid("Supply typeFqn or beanName to look a bean up, or filePath and line to inspect an injection point.")
        }
        if (hasInjection && (filePath == null || line == null)) {
            invalid("An injection point needs both filePath and line.")
        }
        if (column != null && !hasInjection) {
            invalid("column narrows an injection point; it has no meaning for a bean selector.")
        }

        val preference = SOURCES[source] ?: invalid("source must be one of ${SOURCES.keys.joinToString(", ")}.")
        if (contextId != null && preference == BeanSourcePreference.STATIC) {
            invalid("The static model has no loaded context, so contextId cannot apply to it.")
        }
    }

    /** Valid only after [validate]. */
    fun source(): BeanSourcePreference = SOURCES.getValue(source)

    fun mode(): String = if (filePath != null) MODE_INJECTION else MODE_LOOKUP

    /**
     * The question, normalized for the revision fingerprint.
     *
     * Every key is always present so that clearing a filter changes the fingerprint as much as setting one does;
     * a map that simply omitted absent keys would let two different questions hash alike.
     */
    fun normalizedQuery(): Map<String, String?> = mapOf(
        "application" to applicationClassName,
        "source" to source,
        "contextId" to contextId,
        "typeFqn" to typeFqn,
        "beanName" to beanName,
        "filePath" to filePath,
        "line" to line?.toString(),
        "column" to column?.toString(),
        "details" to includeDetails.toString()
    )

    /** A supplied-but-empty argument is one the caller meant to fill in, not one they left out. */
    private fun rejectBlank(value: String?, name: String) {
        if (value != null && value.isBlank()) invalid("$name was supplied empty; omit it or give it a value.")
    }

    private fun invalid(message: String): Nothing =
        throw BeanQueryException(BeanQueryProblem(BoundedBeanResponseWriter.INVALID_ARGUMENT, message))

    companion object {
        const val MODE_LOOKUP = "LOOKUP"
        const val MODE_INJECTION = "INJECTION"

        private val SOURCES = BeanSourcePreference.entries.associateBy { it.name }
    }
}
