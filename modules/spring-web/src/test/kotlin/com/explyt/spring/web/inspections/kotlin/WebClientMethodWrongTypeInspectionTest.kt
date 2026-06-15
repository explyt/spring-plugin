/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.kotlin

import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.WebClientMethodWrongTypeInspection
import org.jetbrains.kotlin.test.TestMetadata

class WebClientMethodWrongTypeInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.kotlin_coroutines_1_7_1
    )

    @TestMetadata("wrongType")
    fun testWrongType() = doTest(WebClientMethodWrongTypeInspection())

}