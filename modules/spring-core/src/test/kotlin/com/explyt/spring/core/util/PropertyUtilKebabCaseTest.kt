/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import junit.framework.TestCase

/**
 * The expectations are taken from `ConventionUtilsTests` of the Spring Boot configuration processor, which is the
 * component that writes the canonical key into the metadata. Any divergence here means the quick fix rewrites a key
 * to a spelling Spring itself would never produce.
 */
class PropertyUtilKebabCaseTest : TestCase() {

    fun testCamelCase() {
        assertEquals("simple-camel-case", PropertyUtil.toKebabCase("simpleCamelCase"))
    }

    fun testUpperCaseSuffixIsSplitPerLetter() {
        assertEquals("my-d-l-q", PropertyUtil.toKebabCase("myDLQ"))
    }

    fun testUpperCaseMiddleIsSplitPerLetter() {
        assertEquals("some-d-l-q-key", PropertyUtil.toKebabCase("someDLQKey"))
    }

    fun testUnderscoreBecomesDash() {
        assertEquals("word-with-underscore", PropertyUtil.toKebabCase("Word_With_underscore"))
    }

    fun testRepeatedUnderscoresArePreservedAsRepeatedDashes() {
        assertEquals("word---with--underscore", PropertyUtil.toKebabCase("Word___With__underscore"))
    }

    fun testUpperCaseAfterUnderscoreIsNotDashedTwice() {
        assertEquals("my-d-l-q", PropertyUtil.toKebabCase("my_DLQ"))
    }

    /**
     * Spring dashes on separators and uppercase letters only. A digit never starts a new word, so `v4` is already
     * canonical; the previous implementation rewrote it to `v-4`.
     */
    fun testDigitDoesNotStartANewWord() {
        assertEquals("v4", PropertyUtil.toKebabCase("v4"))
        assertEquals("s3-logs", PropertyUtil.toKebabCase("s3Logs"))
        assertEquals("oauth2-client", PropertyUtil.toKebabCase("oauth2Client"))
    }

    fun testEverySegmentIsConvertedIndependently() {
        assertEquals(
            "explyt.billing.v4.account-mode-enabled",
            PropertyUtil.toKebabCase("explyt.billing.v4.account_mode_enabled")
        )
    }

    fun testAlreadyCanonicalKeyIsUnchanged() {
        assertEquals("explyt.rate-limit.default-rpm", PropertyUtil.toKebabCase("explyt.rate-limit.default-rpm"))
    }
}
