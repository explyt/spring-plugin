/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.util.SpringCoreUtil.isEqualOrInheritorBeanType
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassType
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
 *
 * Both sides of a type comparison are read in the selected application's classpath. Resolving the beans in one
 * scope and the queried type in another compares the chosen application against another one's class, and the
 * answer looks equally correct either way.
 */
class ScopedBeanMatcher(private val project: Project) {

    fun lookup(snapshot: ScopedBeanSnapshot, selector: BeanLookupSelector): BeanSelection {
        val named = selector.beanName?.let { name ->
            snapshot.records.filter { record ->
                ProgressManager.checkCanceled()
                name in record.knownNames
            }
        } ?: snapshot.records

        val match = selector.typeFqn
            ?.let { typeFqn -> matchType(named, resolveQueryType(typeFqn, snapshot.application)) }
            ?: BeanMatch(named, MatchCompleteness.COMPLETE, 0, emptySet())

        return BeanSelection(outcomeOf(match), match)
    }

    /**
     * Every record whose declared type is compatible with [target], with no preference applied.
     *
     * `@Primary`, `@Priority` and name preference belong to an injection point, which asks which single bean it
     * receives. This asks which beans exist, so applying them would drop exactly the alternatives that make an
     * injection ambiguous. The injection resolver layers its own selection on top of this result.
     *
     * A record whose declared type is absent from the selected classpath is neither kept nor dropped: it is
     * counted as unresolved. Dropping it would report "this bean is not compatible", which the model never
     * established - the class it names simply could not be read in this application's scope.
     */
    fun matchType(records: List<ScopedBeanRecord>, target: PsiType): BeanMatch {
        val matched = mutableListOf<ScopedBeanRecord>()
        val limitations = mutableSetOf<String>()
        var unresolved = 0

        for (record in records) {
            ProgressManager.checkCanceled()
            val declaredType = record.declaredType
            when (isAssignable(declaredType, target)) {
                true -> matched += record
                false -> Unit
                null -> {
                    unresolved++
                    limitations += if (declaredType == null || declaredType.isUnreadable()) {
                        TYPE_NOT_RESOLVABLE_IN_SCOPE
                    } else {
                        TYPE_ARGUMENTS_NOT_COMPARABLE
                    }
                }
            }
        }

        val completeness = if (unresolved > 0) MatchCompleteness.PARTIAL else MatchCompleteness.COMPLETE
        return BeanMatch(matched, completeness, unresolved, limitations)
    }

    /**
     * Reads the queried name in the classpath of the application the snapshot was taken from.
     *
     * The module is looked up by the name the snapshot recorded. When it cannot be found the query is refused
     * rather than answered from a wider scope: a project-wide fallback would silently compare against a class
     * from an application the caller never named, and produce an answer that cannot be told from a correct one.
     */
    private fun resolveQueryType(typeFqn: String, application: BeanApplicationIdentity): PsiType {
        val module = ModuleManager.getInstance(project).findModuleByName(application.moduleName)
            ?: throw BeanQueryException(
                BeanQueryProblem(
                    code = APPLICATION_MODULE_NOT_FOUND,
                    message = "Module '${application.moduleName}' of application " +
                            "'${application.className}' is no longer part of the project"
                )
            )
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
        val classes = JavaPsiFacade.getInstance(project).findClasses(typeFqn, scope)

        val queryClass = when {
            classes.size == 1 -> classes.single()
            classes.isEmpty() -> throw BeanQueryException(
                BeanQueryProblem(
                    code = TYPE_NOT_FOUND,
                    message = "Type '$typeFqn' is not on the classpath of the selected application. " +
                            "Check the fully qualified name, or query by bean name instead"
                )
            )
            // Picking one of several declarations would decide compatibility from a class the caller cannot see.
            else -> throw BeanQueryException(
                BeanQueryProblem(
                    code = TYPE_AMBIGUOUS_IN_SCOPE,
                    message = "Type '$typeFqn' is declared ${classes.size} times in the classpath of the " +
                            "selected application, so a query cannot tell which one it means"
                )
            )
        }
        return JavaPsiFacade.getElementFactory(project).createType(queryClass)
    }

    /**
     * `null` when compatibility could not be decided, which is neither a match nor a mismatch.
     *
     * Compatibility is decided by the project's existing bean-type rule, so a query carrying type arguments is
     * held to them: a `Repository<Bar>` bean does not answer a `Repository<Foo>` query, and a raw query still
     * matches every repository.
     *
     * That rule compares type arguments at one level only. When the bean's own type declares none - a
     * `FooRepository implements Repository<Foo>` against a `Repository<Foo>` query - the argument lives in a
     * supertype the rule does not substitute through, so it reports no match for a bean that may well be one.
     * Reporting that as "not compatible" would state something the model never established, so the record is
     * counted as unresolved and the match becomes PARTIAL. The caller is told the inventory is incomplete
     * instead of being handed a confident answer built on a comparison that did not happen.
     */
    private fun isAssignable(declaredType: PsiType?, target: PsiType): Boolean? {
        if (declaredType == null || declaredType.isUnreadable()) return null
        if (declaredType.isEqualOrInheritorBeanType(target)) return true
        return if (substitutionIsUnprovable(declaredType, target)) null else false
    }

    private fun PsiType.isUnreadable(): Boolean = this is PsiClassType && resolve() == null

    /**
     * Whether the mismatch came from an argument the rule could not substitute rather than from an incompatible
     * type: the bean is a subtype once the arguments are erased, but carries none of its own to compare.
     */
    private fun substitutionIsUnprovable(declaredType: PsiType, target: PsiType): Boolean {
        if (target !is PsiClassType || target.parameterCount == 0) return false
        if (declaredType !is PsiClassType || declaredType.parameterCount > 0) return false
        val declaredClass = declaredType.resolve() ?: return false
        val targetClass = target.resolve() ?: return false
        return InheritanceUtil.isInheritorOrSelf(declaredClass, targetClass, true)
    }

    private fun outcomeOf(match: BeanMatch): BeanOutcome = when {
        match.completeness == MatchCompleteness.PARTIAL -> BeanOutcome.INDETERMINATE
        match.records.isEmpty() -> BeanOutcome.NONE
        match.records.size == 1 -> BeanOutcome.SINGLE
        else -> BeanOutcome.MULTIPLE
    }

    companion object {
        const val TYPE_NOT_FOUND = "TYPE_NOT_FOUND"
        const val TYPE_AMBIGUOUS_IN_SCOPE = "TYPE_AMBIGUOUS_IN_SCOPE"
        const val TYPE_NOT_RESOLVABLE_IN_SCOPE = "TYPE_NOT_RESOLVABLE_IN_SCOPE"
        const val TYPE_ARGUMENTS_NOT_COMPARABLE = "TYPE_ARGUMENTS_NOT_COMPARABLE"
        const val APPLICATION_MODULE_NOT_FOUND = "APPLICATION_MODULE_NOT_FOUND"
    }
}
