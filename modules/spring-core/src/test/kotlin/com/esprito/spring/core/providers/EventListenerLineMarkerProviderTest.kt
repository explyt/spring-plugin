package com.esprito.spring.core.providers

import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.testFramework.TestDataPath

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

        val allLineMarkers = DaemonCodeAnalyzerImpl.getLineMarkers(myFixture.editor.document, project)
//        val eventLineMarkers = allLineMarkers
//            .filter { it.javaClass.simpleName == "RelatedItemLineMarkerInfo" }

        assertEquals(15, allLineMarkers.size)
//        assertEquals(10, eventLineMarkers.size)

    }

}
