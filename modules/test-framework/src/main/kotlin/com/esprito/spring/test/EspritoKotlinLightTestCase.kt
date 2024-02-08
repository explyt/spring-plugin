package com.esprito.spring.test

import com.intellij.testFramework.TestDataPath

private const val TEST_DATA_PATH = "testdata/kotlin/"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
abstract class EspritoKotlinLightTestCase : EspritoBaseLightTestCase() {
    override fun getTestDataPath() = TEST_DATA_PATH
}