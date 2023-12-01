package com.esprito.spring.core.providers

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.util.SpringGutterTestUtil
import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.testFramework.TestDataPath
import junit.framework.TestCase

private const val TEST_DATA_PATH = "testdata/providers/linemarkers"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
class EventListenerLineMarkerProviderTest : EspritoJavaLightTestCase() {
    override fun getTestDataPath(): String = TEST_DATA_PATH
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testBeanAutowired() {
        val vf = myFixture.copyFileToProject(
                "EventListener.java"
        )

        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allEventGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.EventPublisher || it.icon == SpringIcons.EventListener }
        val gutterTargetString = allEventGutters.map { SpringGutterTestUtil.getGutterTargetsStrings(it) }
        TestCase.assertTrue(allEventGutters.isNotEmpty())
        for (targets in gutterTargetString) {
            TestCase.assertTrue(targets.isNotEmpty())
        }
    }

}
