/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core

import junit.framework.TestCase

/**
 * `MessageFormat` treats `'` as a quoting character, so a literal apostrophe has to be doubled — but only in a
 * message that is actually formatted. `BundleBase.postprocessValue` skips `MessageFormat` entirely when no
 * parameters are passed, so a doubled apostrophe in a zero-argument message would reach the user as `''`.
 *
 * These messages are rendered as inspection descriptions, where a stray quote is visible to every user.
 */
class SpringCoreBundleRenderingTest : TestCase() {

    fun testZeroArgumentMessagesRenderApostrophesLiterally() {
        assertEquals(
            "Key is not in Spring's canonical form",
            SpringCoreBundle.message("explyt.spring.inspection.properties.value.should.be.kebab")
        )
    }

    fun testPrefixMessageRendersApostropheAndExample() {
        assertEquals(
            "The prefix must be in Spring's canonical form (lowercase, words separated by '-', such as my.main-project.person)",
            SpringCoreBundle.message("explyt.spring.inspection.config.prefix.kebab")
        )
    }

    fun testQuickFixNameIsCanonicalForm() {
        assertEquals(
            "Switch to canonical form",
            SpringCoreBundle.message("explyt.spring.inspection.properties.key.yaml.fix.case")
        )
    }
}
