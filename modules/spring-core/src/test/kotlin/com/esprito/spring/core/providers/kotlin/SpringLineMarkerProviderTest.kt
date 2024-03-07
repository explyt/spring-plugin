package com.esprito.spring.core.providers.kotlin

import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import org.jetbrains.kotlin.test.TestMetadata

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

@TestMetadata(TEST_DATA_PATH)
class SpringLineMarkerProviderTest : EspritoKotlinLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testBeanAutowired() {
        val vf = myFixture.copyFileToProject(
            "Foo.kt"
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