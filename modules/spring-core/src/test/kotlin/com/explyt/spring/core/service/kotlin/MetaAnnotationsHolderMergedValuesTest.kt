/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.kotlin

import com.explyt.spring.core.service.ComposedBeanNameFixture
import com.explyt.spring.core.service.MergedMetaAnnotationFixture
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary

class MetaAnnotationsHolderMergedValuesTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        ComposedBeanNameFixture.assertSpringCoreMajorVersion(myFixture, module, 6)
        MergedMetaAnnotationFixture.addKotlinCases(myFixture)
    }

    fun testRow1AliasDefaultOnPlainUse() = assertMerged("Case1", "C1", "mode", listOf("A"))

    fun testRow2AliasSetAtUseSite() = assertMerged("Case2", "C1", "mode", listOf("B"))

    fun testRow8cArrayAliasDefault() = assertMerged("Case8c", "C8a", "names", listOf("A"))

    private fun assertMerged(useSite: String, composed: String, attribute: String, expected: List<String>) {
        assertEquals(
            "Merged $attribute of @$composed on $useSite",
            expected,
            MergedMetaAnnotationFixture.mergedValues(myFixture, module, useSite, composed, attribute)
        )
    }
}
