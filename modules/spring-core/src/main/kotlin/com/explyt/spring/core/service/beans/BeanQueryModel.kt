/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.intellij.psi.PsiMember
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable

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

/**
 * How a bean got into the context.
 *
 * `REGISTERED` and `LIBRARY` describe beans a native context reports without a declaration in the project, and
 * `UNKNOWN` is the honest answer when the model does not say - collapsing those into `COMPONENT` would claim a
 * declaration kind nothing established.
 */
enum class BeanKind { COMPONENT, BEAN_METHOD, REGISTERED, LIBRARY, UNKNOWN }

/**
 * One annotation as it is written, never as it would evaluate.
 *
 * [unknownAttributes] names the attributes whose value is not a compile-time constant: reporting them as absent
 * would claim the annotation says less than it does.
 */
data class BeanAnnotationEvidence(
    val annotationClass: String,
    val attributes: Map<String, String>,
    val unknownAttributes: Set<String>,
    val origin: String? = null
)

/**
 * Declaration evidence about one bean, read on demand rather than while enumerating the context.
 *
 * Every field is nullable to separate *unknown* from *known to be absent*: a null [primary] means the model
 * cannot tell, while `false` means the declaration was read and carries no `@Primary`.
 */
data class BeanDetailsEvidence(
    val aliases: List<String>? = null,
    val qualifiers: List<BeanAnnotationEvidence>? = null,
    val primary: Boolean? = null,
    val profiles: List<String>? = null,
    val conditions: List<BeanAnnotationEvidence>? = null,
    val module: String? = null
)

/**
 * One bean of the selected model.
 *
 * [knownNames] holds every name the bean is known to answer to, so an exact-name query can match an alias
 * without a second lookup; [name] is the canonical one. [declaration] and [declaredType] stay null when the
 * model knows the bean but its PSI is not resolvable in the selected application's classpath - dropping such a
 * record would report a bean that exists as absent.
 */
data class ScopedBeanRecord(
    val id: String,
    val name: String,
    val knownNames: Set<String>,
    val typeName: String?,
    val kind: BeanKind,
    val declaration: PsiMember?,
    val declaredType: PsiType?,
    val declarationModule: String?,
    val primary: Boolean?,
    val priority: Int?,
    val details: BeanDetailsEvidence,
    val limitations: Set<String>,
    val runtimeRole: String? = null
)

/**
 * The beans of one chosen model, with what that model cannot promise.
 *
 * [modelStamp] fingerprints the state the records were read from; a continuation computed against a different
 * stamp describes a different model and must not be served as the next page of this one.
 */
data class ScopedBeanSnapshot(
    val application: BeanApplicationIdentity,
    val selection: BeanContextSelection,
    val modelStamp: String,
    val records: List<ScopedBeanRecord>,
    val limitations: Set<String>
)

/** What a lookup asks for. Both filters may be set, and then they intersect rather than widen. */
data class BeanLookupSelector(val typeFqn: String?, val beanName: String?)

/**
 * Whether the match could be computed at all.
 *
 * `PARTIAL` means some record could be neither matched nor excluded - typically a native bean whose class is
 * not resolvable in the selected application's classpath. The known matches are still reported, but no verdict
 * built on completeness may be claimed over them.
 */
enum class MatchCompleteness { COMPLETE, PARTIAL }

/**
 * The records a selector matched, and what could not be decided.
 *
 * [unresolvedCount] counts records whose membership is unknown; it is not an estimate of beans the model never
 * captured.
 */
data class BeanMatch(
    val records: List<ScopedBeanRecord>,
    val completeness: MatchCompleteness,
    val unresolvedCount: Int,
    val limitations: Set<String>
)

/**
 * The verdict of one query, relative to the selected model.
 *
 * `NONE` means nothing matched *in that model* - never that no declaration exists in the repository - and
 * `INDETERMINATE` is returned instead of a confident verdict whenever the match was `PARTIAL`.
 */
enum class BeanOutcome {
    NONE, SINGLE, MULTIPLE, RESOLVED, NO_CANDIDATE, AMBIGUOUS,
    CANDIDATE_SET, DEFERRED, INDETERMINATE
}

data class BeanSelection(val outcome: BeanOutcome, val match: BeanMatch)

/**
 * The dependency form Spring sees at an injection point.
 *
 * `UNKNOWN` is returned whenever the element type cannot be proven - a raw `Optional`, a `Map` with a non-String
 * key, a raw provider - because guessing the form would misreport what Spring resolves.
 */
enum class InjectionShape { SINGLE, OPTIONAL, COLLECTION, PROVIDER, UNKNOWN }

/**
 * What the declaration itself says about a dependency, before any bean is looked up.
 *
 * [required] and [hasDefaultValue] are independent of whether a candidate exists: a missing bean for an optional
 * parameter is not a startup failure, and a present bean does not make a parameter required. `null` means the
 * fact could not be established - never "not required". [basis] names the evidence behind [required] so a caller
 * can tell a proven optional from an unproven one.
 */
data class InjectionFacts(
    val shape: InjectionShape,
    val required: Boolean?,
    val hasDefaultValue: Boolean?,
    val basis: String,
    val limitations: Set<String> = emptySet()
)

/**
 * A supported injection point: the declaration Spring would inject into, and the type it would resolve.
 *
 * [beanType] is the element type behind the shape - `T` of a `List<T>` - which is what a candidate search must
 * match; [declaredType] keeps the type as written.
 */
data class SpringInjectionPoint(
    val name: String,
    val declaredType: PsiType,
    val beanType: PsiType?,
    val variable: PsiVariable,
    val facts: InjectionFacts
)
