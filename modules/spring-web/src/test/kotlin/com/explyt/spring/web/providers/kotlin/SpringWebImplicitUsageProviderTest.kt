/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInspection.deadCode.UnusedDeclarationInspection
import org.jetbrains.kotlin.test.TestMetadata

private const val TEST_DATA_PATH = "suppresses"

@TestMetadata(TEST_DATA_PATH)
class SpringWebImplicitUsageProviderTest : ExplytInspectionKotlinTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springGraphQl_1_0_4
    )

    @TestMetadata("testGraphQl")
    fun testTestGraphQl() = doTest(UnusedDeclarationInspection())
}