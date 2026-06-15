/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.security.inspections.java

import com.explyt.spring.security.inspections.SpringSecurityAnnotationWithUserDetailsInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringSecurityAnnotationWithUserDetailsInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springSecurityTest_6_0_7
    )

    @TestMetadata("withUserDetails")
    fun testWithUserDetails() = doTest(SpringSecurityAnnotationWithUserDetailsInspection())

}