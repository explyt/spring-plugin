/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInspection.inheritance.ImplicitSubclassInspection
import org.jetbrains.kotlin.test.TestMetadata

class FinalDeclarationImplicitSubclassProviderTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springTx_6_0_7
    )

    @TestMetadata("finalDeclaration")
    fun testFinalDeclaration() = doTest(ImplicitSubclassInspection())
}