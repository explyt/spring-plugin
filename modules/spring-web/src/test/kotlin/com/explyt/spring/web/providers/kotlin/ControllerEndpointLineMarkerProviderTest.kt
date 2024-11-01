package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import org.jetbrains.kotlin.test.TestMetadata

private const val TEST_DATA_PATH = "providers/linemarkers"

@TestMetadata(TEST_DATA_PATH)
class ControllerEndpointLineMarkerProviderTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springGraphQl_1_0_4
    )

    fun testYaml() {
        myFixture.configureByFiles("ProductController.kt", "open-api.yaml")
        myFixture.doHighlighting()

        val lineMarkers = DaemonCodeAnalyzerImpl.getLineMarkers(myFixture.editor.document, project)
            .filter { it.lineMarkerTooltip == "Navigate to endpoint usage" }

        assertEquals(1, lineMarkers.size)

        assertEquals(
            "getProduct",
            lineMarkers.map { it.element?.text }
                .firstOrNull()
        )
    }

    fun testJson() {
        myFixture.configureByFiles("ProductController.kt", "open-api.json")
        myFixture.doHighlighting()

        val lineMarkers = DaemonCodeAnalyzerImpl.getLineMarkers(myFixture.editor.document, project)
            .filter { it.lineMarkerTooltip == "Navigate to endpoint usage" }

        assertEquals(1, lineMarkers.size)

        assertEquals(
            "getProduct",
            lineMarkers.map { it.element?.text }
                .firstOrNull()
        )
    }

    fun testMockMvc() {
        myFixture.configureByFiles("ProductController.kt", "ProductControllerTest.kt")
        myFixture.doHighlighting()

        val lineMarkers = DaemonCodeAnalyzerImpl.getLineMarkers(myFixture.editor.document, project)
            .filter { it.lineMarkerTooltip == "Navigate to endpoint usage" }

        assertEquals(1, lineMarkers.size)

        assertEquals(
            "getProduct",
            lineMarkers.map { it.element?.text }
                .firstOrNull()
        )
    }

}