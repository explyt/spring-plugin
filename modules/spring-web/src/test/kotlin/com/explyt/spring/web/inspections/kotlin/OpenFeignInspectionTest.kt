/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.kotlin

import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.OpenFeignInspection
import org.jetbrains.kotlin.test.TestMetadata

class OpenFeignInspectionTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springCloud_4_1_3
    )

    @TestMetadata("openFeign")
    fun testOpenFeign() = doTest(OpenFeignInspection())
}