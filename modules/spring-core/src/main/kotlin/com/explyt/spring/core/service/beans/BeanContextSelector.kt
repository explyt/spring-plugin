/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

/**
 * Picks the one bean model a query is answered from, before any bean is read.
 *
 * Selecting the root up front is what keeps two loaded applications apart: merging their records first and
 * filtering afterwards cannot separate a bean both of them declare. When the choice is not unambiguous the
 * selector reports the alternatives instead of picking one, because answering from the wrong application looks
 * exactly like a correct answer.
 */
object BeanContextSelector {

    fun select(
        application: BeanApplicationIdentity,
        contexts: List<NativeBeanContext>,
        source: BeanSourcePreference,
        contextId: String?
    ): BeanContextSelection {
        if (source == BeanSourcePreference.STATIC) {
            if (contextId != null) {
                throw BeanQueryException(BeanQueryProblem(INVALID_ARGUMENT, "STATIC does not accept contextId."))
            }
            return staticSelection()
        }
        if (contextId != null) return selectById(application, contexts, contextId)

        val matching = contexts.filter { it.belongsTo(application) }
        return when {
            matching.size == 1 -> nativeSelection(matching.single())
            matching.isEmpty() && source == BeanSourcePreference.AUTO ->
                staticSelection(NO_MATCHING_NATIVE_SNAPSHOT)

            matching.isEmpty() -> throw BeanQueryException(
                BeanQueryProblem(NATIVE_CONTEXT_NOT_AVAILABLE, "No loaded context is linked to the application.")
            )

            else -> throw BeanQueryException(
                BeanQueryProblem(
                    CONTEXT_REQUIRED,
                    "Choose one loaded context for the application.",
                    matching.map { it.asChoice() }
                )
            )
        }
    }

    /**
     * An explicit id is looked up among **all** contexts, not only the matching ones: a context that exists but
     * belongs to another application is a different mistake than one that is not loaded, and the caller can only
     * correct it when told which of the two happened.
     */
    private fun selectById(
        application: BeanApplicationIdentity,
        contexts: List<NativeBeanContext>,
        contextId: String
    ): BeanContextSelection {
        val context = contexts.find { it.id == contextId }
            ?: throw BeanQueryException(
                BeanQueryProblem(NATIVE_CONTEXT_NOT_AVAILABLE, "The requested context is not loaded.")
            )
        if (!context.belongsTo(application)) {
            throw BeanQueryException(
                BeanQueryProblem(
                    CONTEXT_APPLICATION_MISMATCH,
                    "The requested context belongs to another application."
                )
            )
        }
        return nativeSelection(context)
    }

    /**
     * A Kotlin `main` facade does not carry the `@SpringBootApplication` FQN, so a context that knows only its
     * linked main file is matched by source identity. An unproven identity never matches: a wrong automatic
     * choice is indistinguishable from a right one in the answer.
     */
    private fun NativeBeanContext.belongsTo(application: BeanApplicationIdentity): Boolean = when {
        !identityProven -> false
        applicationClassName != null -> applicationClassName == application.className
        else -> mainSourceKey != null && mainSourceKey == application.mainSourceKey
    }

    private fun NativeBeanContext.asChoice(): Map<String, String> = mapOf("contextId" to id, "label" to label)

    private fun staticSelection(vararg extraLimitations: String) =
        BeanContextSelection(BeanModelSource.STATIC, null, setOf(STATIC_CONTEXT_APPROXIMATE) + extraLimitations)

    private fun nativeSelection(context: NativeBeanContext) =
        BeanContextSelection(BeanModelSource.NATIVE_SNAPSHOT, context, setOf(NATIVE_SNAPSHOT_NOT_LIVE))

    const val INVALID_ARGUMENT = "INVALID_ARGUMENT"
    const val CONTEXT_REQUIRED = "CONTEXT_REQUIRED"
    const val NATIVE_CONTEXT_NOT_AVAILABLE = "NATIVE_CONTEXT_NOT_AVAILABLE"
    const val CONTEXT_APPLICATION_MISMATCH = "CONTEXT_APPLICATION_MISMATCH"

    const val STATIC_CONTEXT_APPROXIMATE = "STATIC_CONTEXT_APPROXIMATE"
    const val NO_MATCHING_NATIVE_SNAPSHOT = "NO_MATCHING_NATIVE_SNAPSHOT"
    const val NATIVE_SNAPSHOT_NOT_LIVE = "NATIVE_SNAPSHOT_NOT_LIVE"
}
