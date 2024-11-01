package com.explyt.spring.test

import com.intellij.testFramework.TestDataPath

private const val TEST_DATA_PATH = "testdata/kotlin/"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
abstract class ExplytKotlinLightTestCase : ExplytBaseLightTestCase() {
    override fun getTestDataPath() = TEST_DATA_PATH
}