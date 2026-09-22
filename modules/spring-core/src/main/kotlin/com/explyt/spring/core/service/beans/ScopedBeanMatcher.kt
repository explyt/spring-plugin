/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil

/**
 * Finds the beans of an already selected snapshot, by exact name or by assignable type.
 *
 * Nothing here widens: a selector that matches no record answers with no record. The shared injection resolver
 * deliberately does the opposite - when a name matches nothing it falls back to every candidate it knows - which
 * is right for an injection point already narrowed by type, and wrong for a lookup whose whole question is the
 * name. An answer holding someone else's bean is indistinguishable from a correct one.
 *
 * A type query is an inventory and reports every compatible bean. `@Primary` and `@Priority` decide which bean an
 * injection point receives; they do not decide which beans exist, and applying them here would hide from the
 * caller the very alternatives that make an ambiguous injection ambiguous.
 */
class ScopedBeanMatcher(private val project: Project) {

    fun lookup(snapshot: ScopedBeanSnapshot, selector: BeanLookupSelector): BeanSelection {
        val named = selector.beanName?.let { name ->
            snapshot.records.filter { record ->
                ProgressManager.checkCanceled()
                name in record.knownNames
            }
        } ?: snapshot.records

        val match = selector.typeFqn?.let { typeFqn -> matchByType(named, typeFqn) }
            ?: BeanMatch(named, MatchCompleteness.COMPLETE, 0, emptySet())

        return BeanSelection(outcomeOf(match), match)
    }

    /**
     * A record whose declared type is absent from the selected classpath is neither kept nor dropped: it is
     * counted as unresolved. Dropping it would report "this bean is not compatible", which the model never
     * established - the class it names simply could not be read in this application's scope.
     */
    private fun matchByType(records: List<ScopedBeanRecord>, typeFqn: String): BeanMatch {
        val queryClass = findQueryClass(typeFqn)
        val matched = mutableListOf<ScopedBeanRecord>()
        val limitations = mutableSetOf<String>()
        var unresolved = 0

        for (record in records) {
            ProgressManager.checkCanceled()
            when (isAssignable(record.declaredType, queryClass)) {
                true -> matched += record
                false -> Unit
                null -> {
                    unresolved++
                    limitations += TYPE_NOT_RESOLVABLE_IN_SCOPE
                }
            }
        }

        val completeness = if (unresolved > 0) MatchCompleteness.PARTIAL else MatchCompleteness.COMPLETE
        return BeanMatch(matched, completeness, unresolved, limitations)
    }

    private fun findQueryClass(typeFqn: String): PsiClass {
        val scope = GlobalSearchScope.allScope(project)
        return JavaPsiFacade.getInstance(project).findClass(typeFqn, scope)
            ?: throw BeanQueryException(
                BeanQueryProblem(
                    code = TYPE_NOT_FOUND,
                    message = "Type '$typeFqn' is not on the classpath of the selected application. " +
                            "Check the fully qualified name, or query by bean name instead"
                )
            )
    }

    private fun isAssignable(declaredType: PsiType?, queryClass: PsiClass): Boolean? {
        val declaredClass = (declaredType as? com.intellij.psi.PsiClassType)?.resolve() ?: return null
        return InheritanceUtil.isInheritorOrSelf(declaredClass, queryClass, true)
    }

    private fun outcomeOf(match: BeanMatch): BeanOutcome = when {
        match.completeness == MatchCompleteness.PARTIAL -> BeanOutcome.INDETERMINATE
        match.records.isEmpty() -> BeanOutcome.NONE
        match.records.size == 1 -> BeanOutcome.SINGLE
        else -> BeanOutcome.MULTIPLE
    }

    companion object {
        const val TYPE_NOT_FOUND = "TYPE_NOT_FOUND"
        const val TYPE_NOT_RESOLVABLE_IN_SCOPE = "TYPE_NOT_RESOLVABLE_IN_SCOPE"
    }
}
