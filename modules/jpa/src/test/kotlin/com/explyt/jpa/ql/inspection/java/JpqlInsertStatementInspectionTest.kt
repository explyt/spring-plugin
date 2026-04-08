/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.ql.inspection.java

import com.explyt.jpa.ql.inspection.JpqlInsertStatementInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class JpqlInsertStatementInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.jakarta_persistence_3_1_0
    )

    override val realJdk = true

    @TestMetadata("insertStatement")
    fun testInsertStatement() = doTest(JpqlInsertStatementInspection())
}