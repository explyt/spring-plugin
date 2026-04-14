/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.autoconfigure.inspection

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class EnableAutoConfigureSpringFactoryInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    @TestMetadata("propertyCreateFile")
    fun testPropertyCreateFile() {
        doTest(EnableAutoConfigureSpringFactoryInspection())
    }

    @TestMetadata("propertyMoveToFile")
    fun testPropertyMoveToFile() {
        doTest(EnableAutoConfigureSpringFactoryInspection())
    }

    @TestMetadata("propertyRemove")
    fun testPropertyRemove() {
        doTest(EnableAutoConfigureSpringFactoryInspection())
    }
}
