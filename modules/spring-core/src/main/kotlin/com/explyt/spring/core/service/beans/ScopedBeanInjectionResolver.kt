/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation

/**
 * Decides what an injection point receives from an already selected snapshot.
 *
 * The shared [com.explyt.spring.core.service.NativeSearchService.findActiveBeanDeclarations] cannot be reused
 * here: when its own filtering finds nothing it reaches back into the module for array factories and static
 * beans, so a bean the selected context excluded reappears as the answer. Everything below runs over the
 * snapshot's records, and a record that the model could not decide about makes the verdict indeterminate rather
 * than silently dropping out of the count.
 */
class ScopedBeanInjectionResolver(private val project: Project) {

    /** Must run under a read action. */
    fun resolve(snapshot: ScopedBeanSnapshot, point: SpringInjectionPoint): BeanSelection {
        val beanType = point.beanType
            ?: return BeanSelection(
                BeanOutcome.INDETERMINATE,
                BeanMatch(emptyList(), MatchCompleteness.PARTIAL, 0, point.facts.limitations)
            )

        val byType = ScopedBeanMatcher(project).matchType(snapshot.records, beanType)
        val byQualifier = point.variable.getQualifierAnnotation()?.let { narrowByQualifier(byType, it) } ?: byType
        val selected = when (point.facts.shape) {
            InjectionShape.SINGLE, InjectionShape.OPTIONAL -> select(byQualifier)
            else -> byQualifier
        }
        return BeanSelection(outcome(point.facts.shape, selected), selected)
    }

    /**
     * Keeps the candidates the qualifier names, and nothing when it names none of them.
     *
     * The name is compared against every name a record is known to answer to, so a qualifier pointing at an
     * alias selects the same bean its canonical name would. A qualifier whose value is not a constant names
     * nothing this model can read, so the set is reported as undecided rather than silently unfiltered - a
     * caller told "ambiguous" would look for a second bean that the qualifier may well have excluded.
     */
    private fun narrowByQualifier(match: BeanMatch, qualifier: PsiAnnotation): BeanMatch {
        val wanted = qualifier.resolveBeanName()
            ?: return match.copy(
                completeness = MatchCompleteness.PARTIAL,
                limitations = match.limitations + QUALIFIER_NOT_CONSTANT
            )

        val kept = match.records.filter {
            ProgressManager.checkCanceled()
            wanted in it.knownNames
        }
        return match.copy(records = kept)
    }

    /**
     * Narrows several candidates to the one Spring would inject, or leaves them all standing.
     *
     * Only a rule whose inputs are known for *every* competitor may decide: a bean whose `@Primary` flag the
     * model never read could be primary too, and preferring the one that happens to be known would answer from
     * the gap in the model rather than from the configuration. Such a set is reported as indeterminate.
     */
    private fun select(match: BeanMatch): BeanMatch {
        if (match.completeness == MatchCompleteness.PARTIAL || match.records.size <= 1) return match

        if (match.records.any { it.primary == null }) {
            return match.copy(
                completeness = MatchCompleteness.PARTIAL,
                unresolvedCount = match.unresolvedCount + match.records.count { it.primary == null },
                limitations = match.limitations + PRIMARY_UNKNOWN
            )
        }
        match.records.singleOrNull { it.primary == true }?.let { return match.copy(records = listOf(it)) }
        if (match.records.count { it.primary == true } > 1) return match

        return byPriority(match)
    }

    /**
     * `@Priority` orders candidates by the lowest declared value, and only when every candidate declares one:
     * an undeclared priority is not a low one, and a tie is not a winner.
     */
    private fun byPriority(match: BeanMatch): BeanMatch {
        if (match.records.any { it.priority == null }) return match
        val highest = match.records.minOf { it.priority!! }
        val winners = match.records.filter { it.priority == highest }
        return if (winners.size == 1) match.copy(records = winners) else match
    }

    companion object {
        const val PRIMARY_UNKNOWN = "PRIMARY_UNKNOWN"
        const val QUALIFIER_NOT_CONSTANT = "QUALIFIER_NOT_CONSTANT"
    }
}

/**
 * The verdict for one shape over one match.
 *
 * Uncertainty is checked before size: a match that could not be completed says nothing about how many candidates
 * there are, and an unknown shape says nothing about what Spring would do with them. Reporting `NO_CANDIDATE`
 * from an incomplete match would turn "not established" into "proven absent".
 */
internal fun outcome(shape: InjectionShape, match: BeanMatch): BeanOutcome {
    if (match.completeness == MatchCompleteness.PARTIAL || shape == InjectionShape.UNKNOWN) {
        return BeanOutcome.INDETERMINATE
    }
    return when (shape) {
        InjectionShape.COLLECTION -> BeanOutcome.CANDIDATE_SET
        InjectionShape.PROVIDER -> BeanOutcome.DEFERRED
        InjectionShape.SINGLE, InjectionShape.OPTIONAL -> when (match.records.size) {
            0 -> BeanOutcome.NO_CANDIDATE
            1 -> BeanOutcome.RESOLVED
            else -> BeanOutcome.AMBIGUOUS
        }
    }
}
