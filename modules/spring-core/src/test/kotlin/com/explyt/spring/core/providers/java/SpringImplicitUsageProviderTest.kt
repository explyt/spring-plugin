/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInspection.deadCode.UnusedDeclarationInspection
import org.jetbrains.kotlin.test.TestMetadata

private const val TEST_DATA_PATH = "suppresses"

@TestMetadata(TEST_DATA_PATH)
class SpringImplicitUsageProviderTest : ExplytInspectionJavaTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.javax_inject_1,
        TestLibrary.javax_annotation_1_3_2,
        TestLibrary.jakarta_inject_2_0_1,
        TestLibrary.jakarta_annotation_2_1_1,
        TestLibrary.springBootTestAutoConfigure_3_1_1,
    )

    @TestMetadata("testConfiguration")
    fun testTestConfiguration() = doTest(UnusedDeclarationInspection())
}