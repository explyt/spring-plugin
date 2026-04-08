/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.ql.inspection.kotlin

import com.explyt.jpa.ql.inspection.JpqlFullyQualifiedConstructorInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class JpqlFullyQualifiedConstructorInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.jakarta_persistence_3_1_0
    )

    override val realJdk = true

    @TestMetadata("constructorExpression")
    fun testConstructorExpression() = doTest(JpqlFullyQualifiedConstructorInspection())
}