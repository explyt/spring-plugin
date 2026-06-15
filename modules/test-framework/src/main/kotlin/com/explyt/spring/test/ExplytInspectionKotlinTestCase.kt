/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.test

import com.intellij.testFramework.TestDataPath

private const val TEST_DATA_PATH = "testdata/kotlin/inspection/"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
abstract class ExplytInspectionKotlinTestCase : ExplytInspectionBaseTestCase() {

    override fun getTestDataPath(): String = TEST_DATA_PATH

}