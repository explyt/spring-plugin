/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.core.service.beans.BeanApplicationIdentity
import com.explyt.spring.core.service.beans.BeanContextSelection
import com.explyt.spring.core.service.beans.BeanDetailsEvidence
import com.explyt.spring.core.service.beans.BeanKind
import com.explyt.spring.core.service.beans.BeanLookupSelector
import com.explyt.spring.core.service.beans.BeanMatch
import com.explyt.spring.core.service.beans.BeanModelSource
import com.explyt.spring.core.service.beans.BeanOutcome
import com.explyt.spring.core.service.beans.BeanSelection
import com.explyt.spring.core.service.beans.MatchCompleteness
import com.explyt.spring.core.service.beans.NativeBeanContext
import com.explyt.spring.core.service.beans.ScopedBeanRecord
import com.explyt.spring.core.service.beans.ScopedBeanSnapshot
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiMember

/**
 * The projection is a contract: a client parses these field names, and a page continues the previous one only
 * because the order was fixed before anything was projected.
 */
class BeanResponseMapperTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    /** Reordering the input must not reorder the answer, or page two would repeat or skip a record. */
    fun testCandidateOrderDoesNotDependOnInputOrder() {
        val records = listOf(
            record("b-3", "clock", typeName = "java.time.Clock"),
            record("b-1", "alpha", typeName = "java.time.Clock"),
            record("b-2", "alpha", typeName = "com.explyt.demo.Other")
        )

        val forward = idsOf(records)
        val reversed = idsOf(records.reversed())

        assertEquals("Ordered by name, then type: 'com.explyt.demo.Other' precedes 'java.time.Clock'",
            listOf("b-2", "b-1", "b-3"), forward)
        assertEquals(forward, reversed)
    }

    fun testCompactProjectionOmitsDetailsAndKeepsTheFixedFields() {
        val content = map(listOf(record("b-1", "clock")), includeDetails = false)

        val candidate = content.candidateAt(0)
        assertEquals("b-1", candidate["id"].asText())
        assertEquals("clock", candidate["name"].asText())
        assertEquals("java.time.Clock", candidate["type"].asText())
        assertEquals("BEAN_METHOD", candidate["kind"].asText())
        assertFalse("Compact projection must not carry details", candidate.has("details"))
    }

    /** A bean the model knows but whose class it could not resolve keeps its record, minus the declaration. */
    fun testUnknownTypeIsReportedAsNullRatherThanDropped() {
        val content = map(listOf(record("b-1", "clock", typeName = null)), includeDetails = false)

        val candidate = content.candidateAt(0)
        assertEquals(1, content.totalCount)
        assertTrue(candidate["type"].isNull)
        assertFalse(candidate.has("declaration"))
    }

    /** Asking by an alias must still name the canonical bean, and say which name matched. */
    fun testAliasQueryReportsTheMatchedName() {
        val record = record("b-1", "clock", knownNames = setOf("clock", "utcClock"))

        val byAlias = map(listOf(record), includeDetails = false, lookup = BeanLookupSelector(null, "utcClock"))
        val byCanonical = map(listOf(record), includeDetails = false, lookup = BeanLookupSelector(null, "clock"))

        assertEquals("utcClock", byAlias.candidateAt(0)["matchedName"].asText())
        assertEquals("clock", byAlias.candidateAt(0)["name"].asText())
        assertFalse("Canonical name is not a separate match", byCanonical.candidateAt(0).has("matchedName"))
    }

    /**
     * `false` and `[]` are answers; a missing field is not. Collapsing them would report "the model did not
     * look at the declaration" as "the declaration carries no `@Primary`".
     */
    fun testKnownNegativeDetailsSurviveAndUnknownFieldsStayAbsent() {
        val known = record("b-1", "clock", knownNames = emptySet()).copy(primary = false)
        val unknown = record("b-2", "other", knownNames = emptySet())

        val withFacts = map(listOf(known), includeDetails = true).candidateAt(0)["details"]
        val withoutFacts = map(listOf(unknown), includeDetails = true).candidateAt(0)["details"]

        assertTrue("A known-empty alias list must survive", withFacts.has("aliases"))
        assertEquals(0, withFacts["aliases"].size())
        assertEquals("A read 'not primary' is an answer", false, withFacts["primary"].asBoolean())
        assertFalse("An unread profile list must stay absent", withFacts.has("profiles"))
        assertFalse("An unread primary flag must stay absent", withoutFacts.has("primary"))
    }

    fun testModelReportsSourcePrecisionAndSortedLimitations() {
        val content = map(listOf(record("b-1", "clock")), includeDetails = false, native = true)

        val model = content.model
        assertEquals("NATIVE_SNAPSHOT", model["source"].asText())
        assertEquals("SELECTED_SNAPSHOT", model["precision"].asText())
        assertEquals("ctx-1", model["contextId"].asText())
        assertEquals(
            listOf("ALIASES_NOT_EXPORTED", "NATIVE_SNAPSHOT_NOT_LIVE"),
            model["limitations"].map { it.asText() }
        )
    }

    private fun idsOf(records: List<ScopedBeanRecord>): List<String> {
        val content = map(records, includeDetails = false)
        return (0 until content.totalCount).map { content.candidateAt(it)["id"].asText() }
    }

    private fun map(
        records: List<ScopedBeanRecord>,
        includeDetails: Boolean,
        lookup: BeanLookupSelector? = null,
        native: Boolean = false
    ): BeanResponseContent {
        val selection = BeanSelection(
            if (records.size == 1) BeanOutcome.SINGLE else BeanOutcome.MULTIPLE,
            BeanMatch(records, MatchCompleteness.COMPLETE, 0, emptySet())
        )
        return BeanResponseMapper(project).map(snapshotOf(records, native), selection, lookup, null, includeDetails)
    }

    private fun record(
        id: String,
        name: String,
        knownNames: Set<String> = setOf(name),
        typeName: String? = "java.time.Clock",
        declaration: PsiMember? = null
    ) = ScopedBeanRecord(
        id = id,
        name = name,
        knownNames = knownNames,
        typeName = typeName,
        kind = BeanKind.BEAN_METHOD,
        declaration = declaration,
        declaredType = null,
        declarationModule = null,
        primary = null,
        priority = null,
        details = BeanDetailsEvidence(aliases = knownNames.toList()),
        limitations = emptySet()
    )

    private fun snapshotOf(records: List<ScopedBeanRecord>, native: Boolean) = ScopedBeanSnapshot(
        application = BeanApplicationIdentity("com.explyt.demo.App", module.name, "app-source"),
        selection = if (native) {
            BeanContextSelection(
                BeanModelSource.NATIVE_SNAPSHOT,
                NativeBeanContext("ctx-1", "demo-main", "/linked", "com.explyt.demo.App", "app-source", true),
                setOf("NATIVE_SNAPSHOT_NOT_LIVE")
            )
        } else {
            BeanContextSelection(BeanModelSource.STATIC, null, setOf("STATIC_CONTEXT_APPROXIMATE"))
        },
        modelStamp = "stamp",
        records = records,
        limitations = if (native) setOf("ALIASES_NOT_EXPORTED") else emptySet()
    )
}
