/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import org.junit.Assert
import org.junit.Test

/**
 * The boundary rule shared by every lookup that resolves a key with no exact declaration to the declaration that
 * owns it — map entries, collection elements and the inspections that decide whether an unknown key is legal.
 *
 * A plain `startsWith` answers a different question: it accepts `foo.bar` as the owner of `foo.barbaz`, two keys
 * that share seven characters and nothing else. The match has to end where a new segment begins, and the segment
 * opener is `.` for a nested key but `[` for a collection element or a bracket-notation map entry, so neither
 * character alone is the rule.
 */
class PropertyOwnershipBoundaryTest {

    @Test
    fun testDeclarationOwnsItself() {
        Assert.assertTrue(PropertyUtil.isOwnedBy("logging.level", "logging.level"))
    }

    @Test
    fun testDeclarationOwnsANestedSegment() {
        Assert.assertTrue(PropertyUtil.isOwnedBy("logging.level.sql", "logging.level"))
    }

    @Test
    fun testDeclarationOwnsADeeplyNestedSegment() {
        Assert.assertTrue(PropertyUtil.isOwnedBy("logging.level.com.example.Dao", "logging.level"))
    }

    @Test
    fun testDeclarationOwnsAnIndexedElement() {
        Assert.assertTrue(PropertyUtil.isOwnedBy("ingest.s3-logs.sources[0].enabled", "ingest.s3-logs.sources"))
    }

    @Test
    fun testDeclarationOwnsABracketNotationEntry() {
        Assert.assertTrue(PropertyUtil.isOwnedBy("logging.level[com.example.Dao]", "logging.level"))
    }

    /** The defect: the match ends in the middle of the `barbaz` segment, so the two keys are unrelated. */
    @Test
    fun testPrefixEndingInsideASegmentOwnsNothing() {
        Assert.assertFalse(PropertyUtil.isOwnedBy("foo.barbaz", "foo.bar"))
    }

    /** The same defect one level down, where the shared prefix spans several whole segments first. */
    @Test
    fun testPrefixEndingInsideADeeperSegmentOwnsNothing() {
        Assert.assertFalse(PropertyUtil.isOwnedBy("spring.datasource.urls", "spring.datasource.url"))
    }

    /** A collection declaration must not capture elements of a differently named collection. */
    @Test
    fun testPrefixEndingInsideASegmentOwnsNoIndexedElement() {
        Assert.assertFalse(PropertyUtil.isOwnedBy("ingest.sources[0].enabled", "ingest.source"))
    }

    @Test
    fun testUnrelatedKeyIsNotOwned() {
        Assert.assertFalse(PropertyUtil.isOwnedBy("app.custom.key", "logging.level"))
    }

    @Test
    fun testShorterKeyIsNotOwnedByALongerDeclaration() {
        Assert.assertFalse(PropertyUtil.isOwnedBy("logging", "logging.level"))
    }

    /**
     * An empty declared name is not a universal owner. Metadata should never carry one, but the previous
     * `startsWith` accepted it for every key, which is the widest possible false match.
     */
    @Test
    fun testEmptyDeclarationOwnsNothingButItself() {
        Assert.assertFalse(PropertyUtil.isOwnedBy("logging.level", ""))
        Assert.assertTrue(PropertyUtil.isOwnedBy("", ""))
    }

    /**
     * The rule is applied to canonical forms at the call sites that honour relaxed binding, so the boundary
     * characters must survive [PropertyUtil.toCommonPropertyForm] — it strips `-` and `_` but not `.` or `[`.
     */
    @Test
    fun testBoundaryCharactersSurviveCanonicalisation() {
        val declaration = PropertyUtil.toCommonPropertyForm("ingest.s3-logs.sources")
        val key = PropertyUtil.toCommonPropertyForm("ingest.s3-logs.sources[0].enabled")
        Assert.assertTrue(PropertyUtil.isOwnedBy(key, declaration))
        Assert.assertFalse(
            PropertyUtil.isOwnedBy(PropertyUtil.toCommonPropertyForm("ingest.s3-logs.sourcesets"), declaration)
        )
    }
}
