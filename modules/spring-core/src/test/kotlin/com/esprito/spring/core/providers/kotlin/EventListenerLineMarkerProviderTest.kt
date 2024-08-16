package com.esprito.spring.core.providers.kotlin

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.test.util.SpringGutterTestUtil
import org.jetbrains.kotlin.test.TestMetadata

private const val TEST_DATA_PATH = "providers/linemarkers"

@TestMetadata(TEST_DATA_PATH)
class EventListenerLineMarkerProviderTest : EspritoKotlinLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testEventListenerLineMarker() {
        val vf = myFixture.copyFileToProject(
            "EventListener.kt"
        )

        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allEventGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.EventPublisher || it.icon == SpringIcons.EventListener }
        val gutterTargetString = allEventGutters.map { SpringGutterTestUtil.getGutterTargetsStrings(it) }
        assertTrue(allEventGutters.isNotEmpty())
        for (targets in gutterTargetString) {
            assertTrue(targets.isNotEmpty())
        }
    }

}