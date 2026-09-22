/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

/**
 * The matcher answers from the records it is handed and from nothing else.
 *
 * The defect these tests exist for is a widening fallback: the shared injection resolver answers an unmatched
 * name with *every* bean it knows, so a query for a name nobody declares comes back holding someone else's bean.
 * That answer is indistinguishable from a correct one, which is why the name path is pinned here rather than
 * reused from the resolver.
 */
class ScopedBeanMatcherTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testUnknownNameDoesNotFallBackToTheOnlyBean() {
        val snapshot = snapshotOf(record("bean-clock", "clock", setOf("clock", "utcClock")))

        val result = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector(null, "missing"))

        assertEquals(BeanOutcome.NONE, result.outcome)
        assertTrue("An unmatched name must not widen to the whole context", result.match.records.isEmpty())
    }

    fun testUnknownNameStaysEmptyWithAnEmptyAndACrowdedContext() {
        val empty = ScopedBeanMatcher(project).lookup(snapshotOf(), BeanLookupSelector(null, "missing"))
        assertEquals(BeanOutcome.NONE, empty.outcome)

        val crowded = ScopedBeanMatcher(project).lookup(
            snapshotOf(
                record("bean-a", "alpha", setOf("alpha")),
                record("bean-b", "beta", setOf("beta"))
            ),
            BeanLookupSelector(null, "missing")
        )

        assertEquals(BeanOutcome.NONE, crowded.outcome)
        assertTrue(crowded.match.records.isEmpty())
    }

    /** An alias is a name the bean answers to, so it must reach the same record - not a second one. */
    fun testSecondKnownNameFindsTheSameRecord() {
        val snapshot = snapshotOf(record("bean-clock", "clock", setOf("clock", "utcClock")))

        val byCanonical = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector(null, "clock"))
        val byAlias = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector(null, "utcClock"))

        assertEquals(BeanOutcome.SINGLE, byAlias.outcome)
        assertEquals(listOf("bean-clock"), byAlias.match.records.map { it.id })
        assertEquals(byCanonical.match.records.map { it.id }, byAlias.match.records.map { it.id })
    }

    /**
     * Both filters set means both must hold.
     *
     * Two beans share the name, so the name filter alone cannot decide - only the type excludes one. A union
     * would answer with the bean the caller ruled out by type.
     */
    fun testNameAndTypeIntersectInsteadOfWidening() {
        val snapshot = snapshotOf(
            record("bean-clock", "clock", setOf("clock"), declaredType = typeOf("java.time.Clock")),
            record("bean-text", "clock", setOf("clock"), typeName = "java.lang.String", declaredType = typeOf("java.lang.String"))
        )

        val result = ScopedBeanMatcher(project)
            .lookup(snapshot, BeanLookupSelector("java.time.Clock", "clock"))

        assertEquals(BeanOutcome.SINGLE, result.outcome)
        assertEquals(listOf("bean-clock"), result.match.records.map { it.id })
    }

    /**
     * A type query is an inventory: `@Primary` decides which bean an injection point gets, not which beans
     * exist. Narrowing here would hide the alternatives the caller asked to see.
     */
    fun testTypeInventoryKeepsEveryCompatibleBeanIncludingNonPrimary() {
        myFixture.addClass("package com.explyt.demo; public interface Clock {}")
        myFixture.addClass("package com.explyt.demo; public class FastClock implements Clock {}")
        myFixture.addClass("package com.explyt.demo; public class SlowClock implements Clock {}")
        val snapshot = snapshotOf(
            record("bean-fast", "fast", setOf("fast"), "com.explyt.demo.FastClock", typeOf("com.explyt.demo.FastClock"), primary = true),
            record("bean-slow", "slow", setOf("slow"), "com.explyt.demo.SlowClock", typeOf("com.explyt.demo.SlowClock"), primary = false)
        )

        val result = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector("com.explyt.demo.Clock", null))

        assertEquals(BeanOutcome.MULTIPLE, result.outcome)
        assertEquals(setOf("bean-fast", "bean-slow"), result.match.records.map { it.id }.toSet())
        assertEquals(MatchCompleteness.COMPLETE, result.match.completeness)
    }

    /** A type nobody declares is a different answer than a type that exists and matches no bean. */
    fun testUnknownTypeIsReportedAsNotFoundRatherThanEmpty() {
        val snapshot = snapshotOf(record("bean-clock", "clock", setOf("clock"), declaredType = typeOf("java.time.Clock")))

        val failure = queryProblem {
            ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector("com.explyt.demo.NoSuchType", null))
        }

        assertEquals(ScopedBeanMatcher.TYPE_NOT_FOUND, failure.problem.code)
    }

    /**
     * A record whose class is not resolvable in the selected classpath can be neither matched nor excluded.
     *
     * Answering `SINGLE` over the one bean that did resolve would claim the other is not compatible, which the
     * model never established.
     */
    fun testUnresolvedRecordMakesTheMatchPartial() {
        myFixture.addClass("package com.explyt.demo; public interface Clock {}")
        myFixture.addClass("package com.explyt.demo; public class FastClock implements Clock {}")
        val snapshot = snapshotOf(
            record("bean-fast", "fast", setOf("fast"), "com.explyt.demo.FastClock", typeOf("com.explyt.demo.FastClock")),
            record("bean-unavailable", "unavailable", setOf("unavailable"), "com.explyt.demo.UnavailableClock", null)
        )

        val result = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector("com.explyt.demo.Clock", null))

        assertEquals(BeanOutcome.INDETERMINATE, result.outcome)
        assertEquals(MatchCompleteness.PARTIAL, result.match.completeness)
        assertEquals(1, result.match.unresolvedCount)
        assertEquals(listOf("bean-fast"), result.match.records.map { it.id })
    }

    /**
     * An exported name still identifies a bean the classpath cannot resolve: dropping it would report a bean the
     * model knows as absent. The type it would match by stays unproven, so the name answer is complete on its own.
     */
    fun testExactNameAnswersForARecordWithoutPsi() {
        val snapshot = snapshotOf(
            record("bean-unavailable", "unavailable", setOf("unavailable"), "com.explyt.demo.UnavailableClock", null)
        )

        val result = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector(null, "unavailable"))

        assertEquals(BeanOutcome.SINGLE, result.outcome)
        assertEquals(MatchCompleteness.COMPLETE, result.match.completeness)
        assertEquals(listOf("bean-unavailable"), result.match.records.map { it.id })
    }

    private fun queryProblem(action: () -> Unit): BeanQueryException =
        org.junit.Assert.assertThrows(BeanQueryException::class.java) { action() }

    private fun record(
        id: String,
        name: String,
        knownNames: Set<String>,
        typeName: String? = "java.time.Clock",
        declaredType: PsiType? = null,
        primary: Boolean? = null
    ) = ScopedBeanRecord(
        id = id,
        name = name,
        knownNames = knownNames,
        typeName = typeName,
        kind = BeanKind.BEAN_METHOD,
        declaration = null,
        declaredType = declaredType,
        declarationModule = null,
        primary = primary,
        priority = null,
        details = BeanDetailsEvidence(aliases = knownNames.toList(), primary = primary),
        limitations = emptySet()
    )

    private fun snapshotOf(vararg records: ScopedBeanRecord) = ScopedBeanSnapshot(
        application = BeanApplicationIdentity("com.explyt.demo.App", "demo.main", "app-source"),
        selection = BeanContextSelection(BeanModelSource.NATIVE_SNAPSHOT, null, emptySet()),
        modelStamp = "stamp",
        records = records.toList(),
        limitations = emptySet()
    )

    private fun typeOf(fqn: String): PsiType {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
            ?: error("No PSI for $fqn")
        return JavaPsiFacade.getElementFactory(project).createType(psiClass)
    }
}
