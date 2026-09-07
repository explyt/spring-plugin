/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.explyt.spring.core.completion.properties.ConfigurationProperty
import org.junit.Assert
import org.junit.Test

/**
 * The fallback that resolves a key with no exact declaration to the property that owns it. Several declarations can
 * be prefixes of one key; only the longest is the owner, and the previous `find` returned whichever the catalogue
 * listed first — an order that depends on which loader contributed the entry.
 */
class LongestPrefixPropertyTest {

    private fun property(name: String) = ConfigurationProperty(name, null, null, null, null, null, null)

    @Test
    fun testLongestPrefixWinsOverAShorterAncestor() {
        val ancestor = property("logging")
        val owner = property("logging.level")
        Assert.assertEquals(
            owner,
            PropertyUtil.longestPrefixProperty(listOf(ancestor, owner), "logging.level.sql")
        )
    }

    /** The defect: with `find`, catalogue order decided the answer, so the reversed list gave the other result. */
    @Test
    fun testResultDoesNotDependOnCatalogueOrder() {
        val ancestor = property("logging")
        val owner = property("logging.level")
        val key = "logging.level.sql"

        Assert.assertEquals(
            PropertyUtil.longestPrefixProperty(listOf(ancestor, owner), key),
            PropertyUtil.longestPrefixProperty(listOf(owner, ancestor), key)
        )
        Assert.assertEquals(owner, PropertyUtil.longestPrefixProperty(listOf(owner, ancestor), key))
    }

    @Test
    fun testExactDeclarationIsItsOwnOwner() {
        val exact = property("spring.datasource.url")
        Assert.assertEquals(
            exact,
            PropertyUtil.longestPrefixProperty(listOf(property("spring"), exact), "spring.datasource.url")
        )
    }

    @Test
    fun testUnrelatedKeyHasNoOwner() {
        val properties = listOf(property("logging.level"), property("spring.datasource"))
        Assert.assertNull(PropertyUtil.longestPrefixProperty(properties, "app.custom.key"))
    }

    @Test
    fun testEmptyCatalogueHasNoOwner() {
        Assert.assertNull(PropertyUtil.longestPrefixProperty(emptyList(), "logging.level.sql"))
    }

    @Test
    fun testDeepestOfThreeNestedPrefixesWins() {
        val properties = listOf(
            property("management"),
            property("management.endpoint"),
            property("management.endpoint.health")
        )
        Assert.assertEquals(
            properties[2],
            PropertyUtil.longestPrefixProperty(properties, "management.endpoint.health.show-details")
        )
    }

    /**
     * A textual prefix is not an ownership relation: `foo.bar` and `foo.barbaz` are unrelated keys that happen to
     * share their first seven characters. Owning the key requires the match to end where a new segment begins.
     */
    @Test
    fun testPrefixEndingInsideASegmentOwnsNothing() {
        Assert.assertNull(PropertyUtil.longestPrefixProperty(listOf(property("foo.bar")), "foo.barbaz"))
    }

    /**
     * The case the longest-prefix selection cannot mask: the mid-segment match is longer than the genuine ancestor,
     * so length alone hands the key to the wrong declaration.
     */
    @Test
    fun testGenuineAncestorBeatsALongerMidSegmentPrefix() {
        val ancestor = property("foo")
        Assert.assertEquals(
            ancestor,
            PropertyUtil.longestPrefixProperty(listOf(ancestor, property("foo.bar")), "foo.barbaz")
        )
    }

    /** The case it did mask, kept as a regression: the deeper genuine ancestor must still win. */
    @Test
    fun testDeeperGenuineAncestorStillWinsOverAMidSegmentPrefix() {
        val owner = property("foo.barbaz")
        Assert.assertEquals(
            owner,
            PropertyUtil.longestPrefixProperty(listOf(property("foo.bar"), owner), "foo.barbaz.qux")
        )
    }

    /**
     * A collection element opens with `[`, not `.`, so the boundary cannot be the dot alone — requiring one would
     * drop collection ownership entirely.
     */
    @Test
    fun testIndexedKeyIsOwnedByTheCollectionDeclaration() {
        val sources = property("ingest.s3-logs.sources")
        Assert.assertEquals(
            sources,
            PropertyUtil.longestPrefixProperty(
                listOf(property("ingest"), sources), "ingest.s3-logs.sources[0].enabled"
            )
        )
    }

    /** A map entry may be written in bracket notation, which ends the owner's match at `[` as well. */
    @Test
    fun testBracketNotationEntryIsOwnedByTheMapDeclaration() {
        val levels = property("logging.level")
        Assert.assertEquals(
            levels,
            PropertyUtil.longestPrefixProperty(listOf(levels), "logging.level[com.example.Dao]")
        )
    }

    /** A declaration that only shares a prefix with the collection name must not capture its elements. */
    @Test
    fun testIndexedKeyIgnoresAMidSegmentPrefixOfTheCollection() {
        Assert.assertNull(
            PropertyUtil.longestPrefixProperty(listOf(property("ingest.source")), "ingest.sources[0].enabled")
        )
    }
}
