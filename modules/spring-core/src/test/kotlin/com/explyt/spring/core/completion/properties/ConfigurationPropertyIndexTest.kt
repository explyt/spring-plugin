/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties

import com.explyt.spring.core.JavaCoreClasses
import com.explyt.spring.core.PrimitiveTypes
import com.explyt.spring.core.util.PropertyUtil
import org.junit.Assert
import org.junit.Test

/**
 * [ConfigurationPropertyIndex] replaces a linear scan, so every test here is differential: the index must
 * answer exactly what `properties.find { isSameProperty(it.name, query, it.type) }` answered.
 */
class ConfigurationPropertyIndexTest {

    private companion object {
        const val STRING = "java.lang.String"
    }

    private fun property(name: String, type: String? = STRING) =
        ConfigurationProperty(name, null, type, null, null, null, null)

    /** The implementation the index replaces. */
    private fun linearFind(properties: List<ConfigurationProperty>, query: String) =
        properties.find { PropertyUtil.isSameProperty(it.name, query, it.type) }

    private fun assertSameAsLinear(properties: List<ConfigurationProperty>, vararg queries: String) {
        val index = ConfigurationPropertyIndex.of(properties)
        for (query in queries) {
            Assert.assertEquals(
                "index disagrees with the linear scan for '$query'",
                linearFind(properties, query),
                index.findProperty(query)
            )
        }
    }

    @Test
    fun testExactAndRelaxedMatch() {
        val properties = listOf(property("spring.datasource.url"), property("my.main-project.first-name"))
        assertSameAsLinear(
            properties,
            "spring.datasource.url",
            "my.main-project.first-name",
            "my.mainproject.firstname",
            "MY.MAIN_PROJECT.FIRST_NAME",
            "my.main_project.first-name"
        )
    }

    @Test
    fun testUnknownKeyIsNotFound() {
        val properties = listOf(property("spring.datasource.url"))
        assertSameAsLinear(properties, "app.custom.key", "spring.datasource", "spring.datasource.url.extra")
        Assert.assertNull(ConfigurationPropertyIndex.of(properties).findProperty("app.custom.key"))
    }

    @Test
    fun testBooleanPropertyMatchesBothSpellings() {
        val properties = listOf(
            property("server.compression.enabled", JavaCoreClasses.BOOLEAN),
            property("server.http2.enabled", PrimitiveTypes.BOOLEAN)
        )
        // No published metadata writes `is-`, but the catalogue entry is aliased to it, so a key written
        // either way has to resolve to the same entry.
        assertSameAsLinear(
            properties,
            "server.compression.enabled",
            "server.compression.is-enabled",
            "server.http2.enabled",
            "server.http2.is-enabled",
            "SERVER.COMPRESSION.IS_ENABLED"
        )
        Assert.assertEquals(
            properties[0],
            ConfigurationPropertyIndex.of(properties).findProperty("server.compression.is-enabled")
        )
    }

    /**
     * The other direction, and the one that actually happens in a project: no published library metadata is
     * named `is-…`, but a project-derived property can be. `FieldConfigurationPropertyDataRetriever` takes the
     * raw field name, so a Kotlin `val isEnabled: Boolean` reaches the catalogue as `is-enabled`, while Spring
     * binds the same field as `enabled`. Both spellings must therefore find the entry.
     */
    @Test
    fun testCatalogueEntryNamedWithIsPrefixMatchesThePlainSpelling() {
        val properties = listOf(property("app.feature.is-enabled", JavaCoreClasses.BOOLEAN))
        assertSameAsLinear(
            properties,
            "app.feature.is-enabled",
            "app.feature.enabled",
            "APP.FEATURE.ENABLED",
            "app.feature.isenabled"
        )
        Assert.assertEquals(properties[0], ConfigurationPropertyIndex.of(properties).findProperty("app.feature.enabled"))
    }

    /**
     * A camel-cased field name is aliased differently from its kebab-cased form: `isEnabled` does not start with
     * `is-`, so it is prefixed again and normalises to `isisenabled`. Surprising, but it is what the linear scan
     * did, and this refactoring is not the place to change it.
     */
    @Test
    fun testCamelCasedIsFieldOnlyMatchesItself() {
        val properties = listOf(property("app.feature.isEnabled", JavaCoreClasses.BOOLEAN))
        assertSameAsLinear(properties, "app.feature.isEnabled", "app.feature.is-enabled", "app.feature.enabled")
        val index = ConfigurationPropertyIndex.of(properties)
        Assert.assertEquals(properties[0], index.findProperty("app.feature.isEnabled"))
        Assert.assertNull(index.findProperty("app.feature.enabled"))
    }

    @Test
    fun testBooleanAliasDoesNotLeakIntoNonBooleanEntries() {
        val properties = listOf(property("app.flag.enabled", STRING))
        assertSameAsLinear(properties, "app.flag.enabled", "app.flag.is-enabled")
        Assert.assertNull(ConfigurationPropertyIndex.of(properties).findProperty("app.flag.is-enabled"))
    }

    @Test
    fun testFirstDeclarationWinsForARepeatedName() {
        val first = property("spring.datasource.url")
        val second = property("spring.datasource.url")
        val properties = listOf(first, second)
        Assert.assertSame(first, ConfigurationPropertyIndex.of(properties).findProperty("spring.datasource.url"))
    }

    @Test
    fun testEarlierEntryWinsWhenBothMapsAnswer() {
        // `app.thing.is-enabled` (a String) and `app.thing.enabled` (a boolean) both match the query
        // `app.thing.is-enabled`: the first through the plain map, the second through the aliased one.
        // The linear scan returned whichever came first in the list, so order must decide, not map order.
        val stringFirst = listOf(
            property("app.thing.is-enabled", STRING),
            property("app.thing.enabled", JavaCoreClasses.BOOLEAN)
        )
        assertSameAsLinear(stringFirst, "app.thing.is-enabled")
        Assert.assertSame(stringFirst[0], ConfigurationPropertyIndex.of(stringFirst).findProperty("app.thing.is-enabled"))

        val booleanFirst = stringFirst.reversed()
        assertSameAsLinear(booleanFirst, "app.thing.is-enabled")
        Assert.assertSame(
            booleanFirst[0],
            ConfigurationPropertyIndex.of(booleanFirst).findProperty("app.thing.is-enabled")
        )
    }

    @Test
    fun testNameWithoutADotBehavesLikeTheLinearScan() {
        val properties = listOf(property("enabled", JavaCoreClasses.BOOLEAN), property("debug"))
        assertSameAsLinear(properties, "enabled", "is-enabled", "debug", "missing")
    }

    @Test
    fun testDifferentialOverAGeneratedCatalogue() {
        val types = listOf(STRING, JavaCoreClasses.BOOLEAN, PrimitiveTypes.BOOLEAN, "java.lang.Integer")
        val properties = (0 until 400).map { i ->
            // Every fourth catalogue name is written `is-…`, the way a project-derived boolean field arrives.
            val last = if (i % 4 == 0) "is-some-property-$i" else "some-property-$i"
            property("spring.module-${i % 20}.group_${i % 7}.$last", types[i % types.size])
        }
        val queries = buildList {
            properties.forEach {
                add(it.name)
                add(it.name.uppercase())
                add(it.name.replace("-", ""))
                add(it.name.substringBeforeLast(".") + ".is-" + it.name.substringAfterLast("."))
            }
            add("nothing.like.this")
        }
        assertSameAsLinear(properties, *queries.toTypedArray())
    }
}
