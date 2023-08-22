package com.esprito.spring.core.providers

import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.testFramework.TestDataPath


private const val TEST_DATA_PATH = "testdata/providers/linemarkers"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
class SpringLineMarkerProviderTest : EspritoJavaLightTestCase() {

    override fun getTestDataPath(): String = TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testBeanAutowired() {
        val vf = myFixture.copyFileToProject(
            "Foo.java"
        )

        myFixture.configureFromExistingVirtualFile(vf)

        myFixture.doHighlighting()

        val lineMarkers = DaemonCodeAnalyzerImpl.getLineMarkers(myFixture.editor.document, project);

        assertEquals(3, lineMarkers.size)

        assertEquals(
            setOf("Foo", "Bar", "foo"),
            lineMarkers.map { it.element?.text }.toSet()
        )
    }

}