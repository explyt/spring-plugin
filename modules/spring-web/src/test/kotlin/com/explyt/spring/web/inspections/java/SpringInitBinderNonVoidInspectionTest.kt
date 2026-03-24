/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.SpringInitBinderNonVoidInspection
import org.jetbrains.kotlin.test.TestMetadata

class SpringInitBinderNonVoidInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWeb_6_0_7)

    @TestMetadata("initBinderReturnType")
    fun testInitBinderReturnType() = doTest(SpringInitBinderNonVoidInspection())
}