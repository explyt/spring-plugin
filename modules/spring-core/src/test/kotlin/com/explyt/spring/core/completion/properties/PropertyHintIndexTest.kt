/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties

import org.junit.Assert
import org.junit.Test

/**
 * [PropertyHintIndex] replaces `hints.any { it.name == key && … }` and
 * `hints.filter { it.name == key }.distinctBy { it.name }`, so every test compares it against those expressions.
 */
class PropertyHintIndexTest {

    private fun hint(name: String, vararg values: String) =
        PropertyHint(name, values.map { ValueHint(it, null) }, emptyList())

    private fun providerHint(name: String, providerName: String, target: String? = null) =
        PropertyHint(name, emptyList(), listOf(ProviderHint(providerName, target?.let { ProviderParameters(it) })))

    @Test
    fun testHintsNamedReturnsEveryDeclarationInCatalogueOrder() {
        val first = hint("logging.level.keys", "root")
        val second = hint("logging.level.keys", "sql")
        val other = hint("server.port")
        val index = PropertyHintIndex.of(listOf(first, other, second))

        Assert.assertEquals(listOf(first, second), index.hintsNamed("logging.level.keys"))
        Assert.assertEquals(listOf(other), index.hintsNamed("server.port"))
        Assert.assertEquals(emptyList<PropertyHint>(), index.hintsNamed("nothing.declares.this"))
    }

    /**
     * The `any` it replaces looked at *every* hint under the name, not just the first, so a predicate satisfied
     * only by a later declaration still has to match.
     */
    @Test
    fun testAnyMatchesAPredicateSatisfiedOnlyByALaterDeclaration() {
        val hints = listOf(
            providerHint("app.target", "any"),
            providerHint("app.target", "class-reference")
        )
        val index = PropertyHintIndex.of(hints)

        val linear = hints.any { it.name == "app.target" && it.providers.any { p -> p.name == "class-reference" } }
        val indexed = index.hintsNamed("app.target").any { it.providers.any { p -> p.name == "class-reference" } }
        Assert.assertTrue(linear)
        Assert.assertEquals(linear, indexed)
    }

    @Test
    fun testFirstPerNameKeepsOnlyTheFirstDeclarationOfEachName() {
        val firstValues = hint("app.mode", "a")
        val shadowed = hint("app.mode", "b")
        val keys = hint("app.mode.values", "c")
        val hints = listOf(firstValues, shadowed, keys)

        val linear = hints.filter { it.name == "app.mode" || it.name == "app.mode.values" }.distinctBy { it.name }
        Assert.assertEquals(linear, PropertyHintIndex.of(hints).firstPerName("app.mode", "app.mode.values"))
    }

    /** The values end up in a user-visible message, so the catalogue order of the names has to survive. */
    @Test
    fun testFirstPerNameOrdersByCatalogueNotByArgument() {
        val early = hint("app.early", "x")
        val late = hint("app.late", "y")
        val hints = listOf(early, late)
        val index = PropertyHintIndex.of(hints)

        Assert.assertEquals(listOf(early, late), index.firstPerName("app.late", "app.early"))
        Assert.assertEquals(
            hints.filter { it.name == "app.late" || it.name == "app.early" }.distinctBy { it.name },
            index.firstPerName("app.late", "app.early")
        )
    }

    @Test
    fun testFirstPerNameCollapsesARepeatedArgumentLikeDistinctBy() {
        val only = hint("app.mode", "a")
        val hints = listOf(only)
        Assert.assertEquals(listOf(only), PropertyHintIndex.of(hints).firstPerName("app.mode", "app.mode"))
    }

    @Test
    fun testUnknownNamesYieldNothing() {
        val index = PropertyHintIndex.of(listOf(hint("app.mode", "a")))
        Assert.assertEquals(emptyList<PropertyHint>(), index.firstPerName("absent.one", "absent.two"))
    }

    @Test
    fun testDifferentialOverAGeneratedCatalogue() {
        val hints = (0 until 300).map { i ->
            if (i % 3 == 0) hint("app.group-${i % 25}.key-$i", "v$i") else providerHint("app.group-${i % 25}.key-$i", "any")
        } + (0 until 40).map { i -> hint("app.group-${i % 25}.key-$i", "shadow$i") }
        val index = PropertyHintIndex.of(hints)

        for (name in hints.map { it.name }.distinct() + listOf("absent.key")) {
            Assert.assertEquals(
                "hintsNamed disagrees for '$name'",
                hints.filter { it.name == name },
                index.hintsNamed(name)
            )
            val valuesName = name.substringBeforeLast(".") + ".values"
            Assert.assertEquals(
                "firstPerName disagrees for '$name'",
                hints.filter { it.name == name || it.name == valuesName }.distinctBy { it.name },
                index.firstPerName(name, valuesName)
            )
        }
    }
}
