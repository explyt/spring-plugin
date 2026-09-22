/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.service.SpringSearchService

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

/**
 * The injection resolver answers from the snapshot it is handed and from nothing else.
 *
 * The shared [com.explyt.spring.core.service.NativeSearchService.findActiveBeanDeclarations] reaches back into
 * the module whenever its own filtering comes up empty - it looks for array factories and for static beans - so
 * a candidate that the selected context excluded reappears in the answer. That answer cannot be told apart from
 * a correct one, which is why the pipeline here runs over records only.
 */
class ScopedBeanInjectionResolverTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    /**
     * A snapshot without records must answer "no candidate", even when the module still declares a factory the
     * legacy path would fall back to.
     */
    fun testEmptySnapshotDoesNotFallBackToTheModuleArrayFactory() {
        val file = injectionForms()
        val point = resolveAt(file, "ArrayFallbackConsumer(Clock clock", "ArrayFallbackConsumer(Clock ".length)
        @Suppress("DEPRECATION") // The legacy path is the subject here: it is what the resolver must not reach.
        val moduleWideFactories = SpringSearchService.getInstance(project).getComponentBeanPsiMethods(module)
        assertTrue(
            "Precondition: the module must declare the array factory, otherwise the fallback is not exercised",
            moduleWideFactories.any { it.name == "excludedClocks" }
        )

        val result = ScopedBeanInjectionResolver(project).resolve(snapshotOf(), point)

        assertEquals(BeanOutcome.NO_CANDIDATE, result.outcome)
        assertTrue("A module-wide factory must not enter a scoped answer", result.match.records.isEmpty())
    }

    /**
     * Two implementations of one interface make the injection ambiguous; a type lookup over the same records
     * still reports both. The verdicts differ because the questions differ - which bean arrives here, versus
     * which beans exist - and collapsing them would hide the alternatives that make this injection ambiguous.
     */
    fun testTwoImplementationsAreAmbiguousForInjectionAndAnInventoryForLookup() {
        val snapshot = twoClockSnapshot()
        val point = resolveAt(file = injectionForms(), marker = "SingleConsumer(Clock clock", offsetInMarker = "SingleConsumer(Clock ".length)

        val injected = ScopedBeanInjectionResolver(project).resolve(snapshot, point)
        val inventory = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector("java.time.Clock", null))

        assertEquals(BeanOutcome.AMBIGUOUS, injected.outcome)
        assertEquals(setOf("bean-fast", "bean-slow"), injected.match.records.map { it.id }.toSet())
        assertEquals(BeanOutcome.MULTIPLE, inventory.outcome)
    }

    /**
     * One `@Primary` among otherwise equal candidates is what Spring injects, so the point resolves. The
     * inventory over the same records stays `MULTIPLE`: the other bean still exists.
     */
    fun testSinglePrimaryWinsForInjectionWhileTheInventoryKeepsBoth() {
        val snapshot = twoClockSnapshot(primaryFast = true)
        val point = resolveAt(injectionForms(), "SingleConsumer(Clock clock", "SingleConsumer(Clock ".length)

        val injected = ScopedBeanInjectionResolver(project).resolve(snapshot, point)
        val inventory = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector("java.time.Clock", null))

        assertEquals(BeanOutcome.RESOLVED, injected.outcome)
        assertEquals(listOf("bean-fast"), injected.match.records.map { it.id })
        assertEquals(BeanOutcome.MULTIPLE, inventory.outcome)
    }

    /**
     * A `@Qualifier` names the bean, so two candidates of one type stop being ambiguous. The name is matched
     * against the record's known names, which is what makes an alias work here without a second lookup.
     */
    fun testQualifierSelectsOneOfTwoCandidates() {
        val point = resolveAt(injectionForms(), "@Qualifier(\"slow\") Clock qualified", "@Qualifier(\"slow\") Clock ".length)

        val result = ScopedBeanInjectionResolver(project).resolve(twoClockSnapshot(), point)

        assertEquals(BeanOutcome.RESOLVED, result.outcome)
        assertEquals(listOf("bean-slow"), result.match.records.map { it.id })
    }

    /** A qualifier naming nothing in this context resolves to nothing - it must not fall back to the set. */
    fun testUnmatchedQualifierLeavesNoCandidate() {
        val point = resolveAt(injectionForms(), "@Qualifier(\"missing\") Clock unmatched", "@Qualifier(\"missing\") Clock ".length)

        val result = ScopedBeanInjectionResolver(project).resolve(twoClockSnapshot(), point)

        assertEquals(BeanOutcome.NO_CANDIDATE, result.outcome)
        assertTrue(result.match.records.isEmpty())
    }

    /** Two `@Primary` beans are a configuration Spring rejects; naming one of them would invent a rule. */
    fun testTwoPrimariesStayAmbiguous() {
        val snapshot = twoClockSnapshot(primaryFast = true, primarySlow = true)
        val point = resolveAt(injectionForms(), "SingleConsumer(Clock clock", "SingleConsumer(Clock ".length)

        val result = ScopedBeanInjectionResolver(project).resolve(snapshot, point)

        assertEquals(BeanOutcome.AMBIGUOUS, result.outcome)
        assertEquals(setOf("bean-fast", "bean-slow"), result.match.records.map { it.id }.toSet())
    }

    /**
     * A competitor whose primary flag is unknown cannot be ruled out, so the known primary does not settle the
     * point. Selecting it would turn "nothing says otherwise" into "nothing else qualifies".
     */
    fun testPrimaryDoesNotWinOverACompetitorWithUnknownPrimaryFlag() {
        val snapshot = twoClockSnapshot(primaryFast = true, primarySlow = null)
        val point = resolveAt(injectionForms(), "SingleConsumer(Clock clock", "SingleConsumer(Clock ".length)

        val result = ScopedBeanInjectionResolver(project).resolve(snapshot, point)

        assertEquals(BeanOutcome.INDETERMINATE, result.outcome)
    }

    /**
     * A collection receives the whole set, so `@Primary` must not narrow it: the one bean it would select for a
     * single-valued point is not the answer to "give me all of them".
     */
    fun testCollectionKeepsEveryCandidateEvenWithAPrimaryAmongThem() {
        val point = resolveAt(injectionForms(), "private List<Clock> allClocks", "private ".length)
        val withPrimary = twoClockSnapshot(primaryFast = true)
        assertEquals(
            "Precondition: the same records must resolve to one bean at a single-valued point, " +
                    "otherwise this proves nothing about the collection",
            BeanOutcome.RESOLVED,
            ScopedBeanInjectionResolver(project)
                .resolve(withPrimary, resolveAt(injectionForms(), "SingleConsumer(Clock clock", "SingleConsumer(Clock ".length))
                .outcome
        )

        val result = ScopedBeanInjectionResolver(project).resolve(withPrimary, point)

        assertEquals(BeanOutcome.CANDIDATE_SET, result.outcome)
        assertEquals(
            "A primary bean must not shrink a collection injection",
            setOf("bean-fast", "bean-slow"), result.match.records.map { it.id }.toSet()
        )
    }

    /** An empty and a single-element collection are still a set: the shape decides the verdict, not the count. */
    fun testCollectionStaysACandidateSetWhenEmptyOrSingle() {
        val file = injectionForms()
        val point = resolveAt(file, "private List<Clock> allClocks", "private ".length)

        val empty = ScopedBeanInjectionResolver(project).resolve(snapshotOf(), point)
        val single = ScopedBeanInjectionResolver(project).resolve(snapshotOf(fastClock()), point)

        assertEquals(BeanOutcome.CANDIDATE_SET, empty.outcome)
        assertTrue(empty.match.records.isEmpty())
        assertEquals(BeanOutcome.CANDIDATE_SET, single.outcome)
        assertEquals(listOf("bean-fast"), single.match.records.map { it.id })
    }

    /** `Optional` narrows like a single-valued point, so two candidates are as ambiguous as they would be there. */
    fun testOptionalWithTwoCandidatesIsAmbiguous() {
        val point = resolveAt(injectionForms(), "private Optional<Clock> optionalClock", "private ".length)

        val result = ScopedBeanInjectionResolver(project).resolve(twoClockSnapshot(), point)

        assertEquals(BeanOutcome.AMBIGUOUS, result.outcome)
    }

    /**
     * A provider defers the lookup to call time, so neither an empty nor a crowded context is a verdict about
     * it. `DEFERRED` reports the candidates without promising what `getObject()` will return.
     */
    fun testProviderIsDeferredWithNoCandidatesAndWithSeveral() {
        val file = injectionForms()
        val point = resolveAt(file, "private ObjectProvider<Clock> clockProvider", "private ".length)

        val empty = ScopedBeanInjectionResolver(project).resolve(snapshotOf(), point)
        val crowded = ScopedBeanInjectionResolver(project).resolve(twoClockSnapshot(), point)

        assertEquals(BeanOutcome.DEFERRED, empty.outcome)
        assertEquals(BeanOutcome.DEFERRED, crowded.outcome)
        assertEquals(setOf("bean-fast", "bean-slow"), crowded.match.records.map { it.id }.toSet())
    }

    /**
     * A raw provider hides its element type, so there is nothing to search candidates by. `DEFERRED` would
     * claim the point was understood; the shape was not.
     */
    fun testRawContainerIsIndeterminateRatherThanDeferred() {
        val point = resolveAt(injectionForms(), "private Optional rawOptional", "private ".length)

        val result = ScopedBeanInjectionResolver(project).resolve(twoClockSnapshot(), point)

        assertEquals(InjectionShape.UNKNOWN, point.facts.shape)
        assertEquals(BeanOutcome.INDETERMINATE, result.outcome)
    }

    /**
     * A record whose class cannot be read in this classpath is neither a candidate nor ruled out. The known
     * primary next to it does not settle the question - it only looks like it does.
     */
    fun testUnreadableRecordBesideAPrimaryIsIndeterminateNotResolved() {
        val point = resolveAt(injectionForms(), "SingleConsumer(Clock clock", "SingleConsumer(Clock ".length)
        val snapshot = snapshotOf(
            fastClock(primary = true),
            record("bean-unreadable", "unreadable", setOf("unreadable"), "com.explyt.demo.Unreadable", null)
        )

        val result = ScopedBeanInjectionResolver(project).resolve(snapshot, point)

        assertEquals(BeanOutcome.INDETERMINATE, result.outcome)
        assertEquals(MatchCompleteness.PARTIAL, result.match.completeness)
    }

    private fun fastClock(primary: Boolean? = null) = record(
        "bean-fast", "fast", setOf("fast"), "java.time.Clock", typeOf("java.time.Clock"), primary
    )

    private fun twoClockSnapshot(primaryFast: Boolean? = false, primarySlow: Boolean? = false): ScopedBeanSnapshot {
        myFixture.addClass("package com.explyt.demo; public class FastClock extends java.time.Clock { public java.time.ZoneId getZone() { return null; } public java.time.Clock withZone(java.time.ZoneId z) { return null; } public java.time.Instant instant() { return null; } }")
        myFixture.addClass("package com.explyt.demo; public class SlowClock extends java.time.Clock { public java.time.ZoneId getZone() { return null; } public java.time.Clock withZone(java.time.ZoneId z) { return null; } public java.time.Instant instant() { return null; } }")
        return snapshotOf(
            record("bean-fast", "fast", setOf("fast"), "com.explyt.demo.FastClock", typeOf("com.explyt.demo.FastClock"), primary = primaryFast),
            record("bean-slow", "slow", setOf("slow"), "com.explyt.demo.SlowClock", typeOf("com.explyt.demo.SlowClock"), primary = primarySlow)
        )
    }

    private fun record(
        id: String,
        name: String,
        knownNames: Set<String>,
        typeName: String? = "java.time.Clock",
        declaredType: PsiType? = null,
        primary: Boolean? = null,
        priority: Int? = null
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
        priority = priority,
        details = BeanDetailsEvidence(aliases = knownNames.toList(), primary = primary),
        limitations = emptySet()
    )

    /**
     * A column that lands inside several overlapping declarations names none of them, so the caller is asked
     * rather than served whichever came first - answering about the wrong parameter reads exactly like
     * answering about the right one.
     */
    fun testColumnInsideSeveralOverlappingDeclarationsAsksInsteadOfPickingOne() {
        val file = injectionForms()
        val offset = file.text.indexOf("Clock injected, Clock second")
        assertTrue("Precondition: the two-parameter method must exist in the fixture", offset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        val line = document.getLineNumber(offset) + 1

        val failure = org.junit.Assert.assertThrows(BeanQueryException::class.java) {
            SpringInjectionPointResolver(project).resolve(file, line, null)
        }

        assertEquals(SpringInjectionPointResolver.INJECTION_POINT_REQUIRED, failure.problem.code)
        assertEquals(setOf("injected", "second"), failure.problem.choices.mapNotNull { it["name"] }.toSet())
    }

    private fun injectionForms(): PsiFile {
        val file = myFixture.copyFileToProject("beanQuery/InjectionForms.java", "com/explyt/demo/InjectionForms.java")
        val psiFile = myFixture.psiManager.findFile(file)
        assertNotNull("Precondition: the fixture must load, otherwise nothing is proven", psiFile)
        return psiFile!!
    }

    private fun resolveAt(file: PsiFile, marker: String, offsetInMarker: Int = 0): SpringInjectionPoint {
        val offset = file.text.indexOf(marker)
        assertTrue("Precondition: marker '$marker' must exist in the fixture", offset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        val target = offset + offsetInMarker
        val line = document.getLineNumber(target) + 1
        val column = target - document.getLineStartOffset(line - 1) + 1
        return SpringInjectionPointResolver(project).resolve(file, line, column)
    }

    private fun snapshotOf(vararg records: ScopedBeanRecord) = ScopedBeanSnapshot(
        application = BeanApplicationIdentity("com.explyt.demo.App", module.name, "app-source"),
        selection = BeanContextSelection(BeanModelSource.STATIC, null, emptySet()),
        modelStamp = "stamp",
        records = records.toList(),
        limitations = emptySet()
    )

    private fun typeOf(fqn: String, vararg argumentFqns: String): PsiType {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
            ?: error("No PSI for $fqn")
        val arguments = argumentFqns.map { typeOf(it) }.toTypedArray()
        return JavaPsiFacade.getElementFactory(project).createType(psiClass, *arguments)
    }
}
