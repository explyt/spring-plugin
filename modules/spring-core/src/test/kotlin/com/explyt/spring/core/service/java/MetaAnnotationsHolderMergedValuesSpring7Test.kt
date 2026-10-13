/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.java

import com.explyt.spring.core.service.ComposedBeanNameFixture
import com.explyt.spring.core.service.MergedMetaAnnotationFixture
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary

class MetaAnnotationsHolderMergedValuesSpring7Test : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary("org.springframework:spring-context:7.0.9"))

    override fun setUp() {
        super.setUp()
        ComposedBeanNameFixture.assertSpringCoreMajorVersion(myFixture, module, 7)
        MergedMetaAnnotationFixture.addJavaCases(myFixture)
    }

    fun testRow1AliasDefaultOnPlainUse() = assertMerged("Case1", "C1", "mode", listOf("A"))

    fun testRow2AliasSetAtUseSite() = assertMerged("Case2", "C1", "mode", listOf("B"))

    fun testRow3aConventionDefaultIsIgnored() = assertMerged("Case3a", "C3", "mode", listOf("T"))

    fun testRow3bConventionSetAtUseSiteIsIgnored() = assertMerged("Case3b", "C3", "mode", listOf("T"))

    fun testRow3cMetaFixedValueWinsOverConventionDefault() = assertMerged("Case3c", "C3m", "mode", listOf("META"))

    fun testRow4aOuterWithoutAttributesKeepsInnerAliasDefault() = assertMerged("Case4a", "O4a", "mode", listOf("A"))

    fun testRow4bOuterAliasDefault() = assertMerged("Case4b", "O4b", "mode", listOf("O"))

    fun testRow4cOuterAliasSetAtUseSite() = assertMerged("Case4c", "O4b", "mode", listOf("X"))

    fun testRow5aTwoMetaPathsNearestFirstDeclaredCarriesOuterAliasDefault() =
        assertMerged("Case5a", "O5", "mode", listOf("O"))

    fun testRow5cTwoMetaPathsFirstDeclaredWinsOverAliasedPath() = assertMerged("Case5c", "O5r", "mode", listOf("PB"))

    fun testRow5eTwoMetaPathsWithoutAliasFirstDeclaredWins() = assertMerged("Case5e", "O5n", "mode", listOf("PA"))

    fun testRow6aAliasToIntermediateAttributeOverMetaFixedValue() = assertMerged("Case6a", "O6", "mode", listOf("O"))

    fun testRow7aValueAliasPositional() = assertMerged("Case7a", "C7", "mode", listOf("B"))

    fun testRow7bValueAliasDefaultOnPlainUse() = assertMerged("Case7b", "C7", "mode", listOf("A"))

    fun testRow7eValueWithoutAliasIsNeverConventionMapped() = assertMerged("Case7e", "C7v", "mode", listOf("T"))

    fun testRow8aMetaFixedArrayWinsOverConventionDefault() = assertMerged("Case8a", "C8", "names", listOf("META"))

    fun testRow8bMetaFixedArrayWinsOverConventionSetAtUseSite() =
        assertMerged("Case8b", "C8", "names", listOf("META"))

    fun testRow8cArrayAliasDefault() = assertMerged("Case8c", "C8a", "names", listOf("A"))

    fun testRow9aEmptyArrayAliasDefaultOverMetaFixedValue() = assertMerged("Case9a", "E1", "names", emptyList())

    fun testRow9bEmptyStringAliasDefaultOverMetaFixedValue() = assertMerged("Case9b", "E2", "mode", listOf(""))

    fun testRow9cMetaFixedValueWinsOverEmptyConventionDefault() =
        assertMerged("Case9c", "E3", "mode", listOf("META"))

    private fun assertMerged(useSite: String, composed: String, attribute: String, expected: List<String>) {
        assertEquals(
            "Merged $attribute of @$composed on $useSite",
            expected,
            MergedMetaAnnotationFixture.mergedValues(myFixture, module, useSite, composed, attribute)
        )
    }
}
