/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

/** Which bean model the caller asks for. `AUTO` prefers a native snapshot and falls back to the static model. */
enum class BeanSourcePreference { AUTO, STATIC, NATIVE }

/** Which bean model actually answered. Never `AUTO`: the preference is resolved before the answer is built. */
enum class BeanModelSource { STATIC, NATIVE_SNAPSHOT }

/**
 * An expected selection problem, reported to the caller instead of a guessed answer.
 *
 * [choices] carries the alternatives the caller has to pick between - applications or contexts - so an ambiguity
 * can be resolved by a second call rather than by the tool choosing silently.
 */
data class BeanQueryProblem(
    val code: String,
    val message: String,
    val choices: List<Map<String, String>> = emptyList()
)

class BeanQueryException(val problem: BeanQueryProblem) : RuntimeException(problem.code)

/**
 * The application a query is scoped to.
 *
 * [mainSourceKey] identifies the declaring source file. A Kotlin `main` facade does not carry the
 * `@SpringBootApplication` FQN, so file identity is the only link some native roots can be matched by.
 */
data class BeanApplicationIdentity(
    val className: String,
    val moduleName: String,
    val mainSourceKey: String
)

/**
 * One loaded native root.
 *
 * [identityProven] is false when the root cannot be tied to an application beyond doubt; such a root is never
 * selected automatically, because answering from the wrong application is worse than reporting an ambiguity.
 */
data class NativeBeanContext(
    val id: String,
    val label: String,
    val linkedPath: String,
    val applicationClassName: String?,
    val mainSourceKey: String?,
    val identityProven: Boolean
)

/** The resolved model for one query: which source answers it, which native root, and what it cannot promise. */
data class BeanContextSelection(
    val source: BeanModelSource,
    val nativeContext: NativeBeanContext?,
    val limitations: Set<String>
)
