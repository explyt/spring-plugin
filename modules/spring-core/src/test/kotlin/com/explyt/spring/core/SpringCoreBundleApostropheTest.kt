/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core

import junit.framework.TestCase
import java.util.PropertyResourceBundle

/**
 * `BundleBase.postprocessValue` runs `MessageFormat` only when the caller passes arguments, so the two apostrophe
 * spellings are not interchangeable:
 *
 * - a **zero-argument** key must use a single `'`, because nothing collapses `''` and it reaches the UI literally;
 * - a **parameterized** key must double it, because `MessageFormat` swallows a single `'` and can even treat the
 *   rest of the message as a quoted literal.
 *
 * Neither mistake has compile-time protection, so every key is rendered here and checked for a surviving `''`.
 */
class SpringCoreBundleApostropheTest : TestCase() {

    fun testNoRenderedMessageContainsADoubledApostrophe() {
        val offenders = bundleKeys().mapNotNull { key ->
            val rendered = render(key)
            if (rendered.contains(DOUBLED_APOSTROPHE)) "$key -> $rendered" else null
        }

        assertTrue(
            "A rendered message must never contain a doubled apostrophe. " +
                    "A zero-argument key needs a single ' because MessageFormat does not run for it:\n" +
                    offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }

    /**
     * The inverse mistake: a single `'` in a parameterized message makes `MessageFormat` treat what follows as a
     * quoted literal, so the placeholder is emitted verbatim instead of being substituted.
     */
    fun testEveryPlaceholderOfAParameterizedMessageIsSubstituted() {
        val offenders = bundleKeys().mapNotNull { key ->
            val raw = rawValue(key)
            val placeholders = placeholderCount(raw)
            if (placeholders == 0) return@mapNotNull null

            val rendered = render(key)
            val unsubstituted = (0 until placeholders).filter { rendered.contains("{$it}") }
            if (unsubstituted.isEmpty()) null else "$key -> $rendered"
        }

        assertTrue(
            "A placeholder was not substituted, which happens when an apostrophe in a parameterized message is " +
                    "not doubled:\n" + offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }

    private fun render(key: String): String {
        val arguments = Array<Any>(placeholderCount(rawValue(key))) { "x" }
        return SpringCoreBundle.message(key, *arguments)
    }

    private fun placeholderCount(value: String): Int {
        val indexes = PLACEHOLDER_REGEX.findAll(value).map { it.groupValues[1].toInt() }.toList()
        return if (indexes.isEmpty()) 0 else indexes.max() + 1
    }

    private fun rawValue(key: String): String = BUNDLE.getString(key)

    private fun bundleKeys(): List<String> = BUNDLE.keys.toList().sorted()

    private companion object {
        const val DOUBLED_APOSTROPHE = "''"
        val PLACEHOLDER_REGEX = Regex("""\{(\d+)""")

        val BUNDLE: PropertyResourceBundle = SpringCoreBundleApostropheTest::class.java
            .getResourceAsStream("/messages/SpringCoreBundle.properties")
            .use { PropertyResourceBundle(requireNotNull(it) { "SpringCoreBundle.properties not found" }) }
    }
}
