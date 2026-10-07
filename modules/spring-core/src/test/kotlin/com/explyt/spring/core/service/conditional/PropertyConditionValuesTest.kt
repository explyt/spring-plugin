/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.tracker.ModificationTrackerManager

class PropertyConditionValuesTest : ConditionalOnPropertyBootSemanticsTestCase() {

    fun testEscapedPrefixIsUnresolvable() {
        addProperties("application.yaml", "x: \\${'$'}{A}")

        assertEquals(ConditionPropertyValue.Unresolvable, valueOf("x"))
    }

    fun testEmbeddedEscapedPrefixIsUnresolvable() {
        addProperties("application.yaml", "x: prefix-\\${'$'}{A}", "A: true")

        assertEquals(ConditionPropertyValue.Unresolvable, valueOf("x"))
    }

    fun testEscapedPrefixInDefaultIsUnresolvable() {
        addProperties("application.yaml", "x: ${'$'}{X:\\${'$'}{Y}}")

        assertEquals(ConditionPropertyValue.Unresolvable, valueOf("x"))
    }

    fun testEscapedSeparatorIsUnresolvable() {
        addProperties("application.yaml", "x: ${'$'}{A\\:B:true}")

        assertEquals(ConditionPropertyValue.Unresolvable, valueOf("x"))
    }

    fun testNestedDefaultResolvesWhenNeitherIsDefined() {
        addProperties("application.yaml", "x: ${'$'}{outer.flag:${'$'}{inner.flag:true}}")

        assertEquals(ConditionPropertyValue.Known("true"), valueOf("x"))
    }

    fun testNestedDefaultYieldsToDefinedOuterValue() {
        addProperties("application.yaml", "x: ${'$'}{outer.flag:${'$'}{inner.flag:true}}", "outer.flag: false")

        assertEquals(ConditionPropertyValue.Known("false"), valueOf("x"))
    }

    fun testDefinedReferenceResolves() {
        addProperties("application.yaml", "x: ${'$'}{A}", "A: true")

        assertEquals(ConditionPropertyValue.Known("true"), valueOf("x"))
    }

    fun testPropertiesValueWithLeadingSpaceIsTrimmed() {
        addProperties("application.properties", "x.v= true")

        assertEquals(ConditionPropertyValue.Known("true"), valueOf("x.v"))
    }

    fun testPropertiesUnicodeEscapeIsDecoded() {
        addProperties("application.properties", "x.v=tr\\u0075e")

        assertEquals(ConditionPropertyValue.Known("true"), valueOf("x.v"))
    }

    fun testPropertiesLineContinuationIsJoined() {
        addProperties("application.properties", "x.v=tr\\", "  ue")

        assertEquals(ConditionPropertyValue.Known("true"), valueOf("x.v"))
    }

    fun testLaterDefaultDocumentOverridesEarlierOne() {
        addProperties(
            "application.properties",
            "x.v=a", "#---", "x.v=b", "#---", "spring.config.activate.on-profile=prod", "x.v=c"
        )

        assertEquals(ConditionPropertyValue.Known("b"), valueOf("x.v"))
    }

    fun testThirdDocumentWithActiveProfileWins() {
        addProperties(
            "application.properties",
            "spring.profiles.active=prod", "x.v=a", "#---", "x.v=b", "#---", "spring.config.activate.on-profile=prod", "x.v=c"
        )

        assertEquals(ConditionPropertyValue.Known("c"), valueOf("x.v"))
    }

    fun testLaterOfTwoActiveDocumentsWins() {
        addProperties(
            "application.properties",
            "spring.profiles.active=prod",
            "#---", "spring.config.activate.on-profile=prod", "x.v=b",
            "#---", "spring.config.activate.on-profile=prod", "x.v=c"
        )

        assertEquals(ConditionPropertyValue.Known("c"), valueOf("x.v"))
    }

    fun testFirstDocumentProfileApplies() {
        addProperties("application.properties", "spring.config.activate.on-profile=prod", "x.v=a", "#---", "x.v=b")

        assertEquals(ConditionPropertyValue.Known("b"), valueOf("x.v"))
    }

    fun testSeparatorWithTrailingTextIsNotADocumentBoundary() {
        addProperties("application.properties", "x.only=a", "#---text", "spring.config.activate.on-profile=prod")

        assertEquals(ConditionPropertyValue.Missing, valueOf("x.only"))
    }

    fun testSeparatorNextToCommentIsUnresolvable() {
        addProperties("application.properties", "x.v=a", "# note", "#---", "x.v=b")

        assertEquals(ConditionPropertyValue.Unresolvable, valueOf("x.v"))
    }

    private fun valueOf(key: String): ConditionPropertyValue {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return PropertyConditionValues(module).valueOf(key)
    }
}
