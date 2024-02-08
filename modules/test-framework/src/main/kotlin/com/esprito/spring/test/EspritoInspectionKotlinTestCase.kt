package com.esprito.spring.test

import com.intellij.testFramework.TestDataPath

private const val TEST_DATA_PATH = "testdata/kotlin/inspection/"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
abstract class EspritoInspectionKotlinTestCase : EspritoInspectionBaseTestCase() {

    override fun getTestDataPath(): String = TEST_DATA_PATH

}