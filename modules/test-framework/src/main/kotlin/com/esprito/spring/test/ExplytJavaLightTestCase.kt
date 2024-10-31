package com.esprito.spring.test

import com.intellij.testFramework.TestDataPath

private const val TEST_DATA_PATH = "testdata/java/"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
abstract class ExplytJavaLightTestCase : ExplytBaseLightTestCase() {
    override fun getTestDataPath() = TEST_DATA_PATH
}