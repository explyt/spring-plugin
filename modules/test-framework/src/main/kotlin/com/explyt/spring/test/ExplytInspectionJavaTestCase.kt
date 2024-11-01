package com.explyt.spring.test

import com.intellij.testFramework.TestDataPath

private const val TEST_DATA_PATH = "testdata/java/inspection/"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
abstract class ExplytInspectionJavaTestCase : ExplytInspectionBaseTestCase() {

    override fun getTestDataPath(): String = TEST_DATA_PATH

}