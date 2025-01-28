/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.icons.AllIcons
import org.jetbrains.kotlin.test.TestMetadata

private const val TEST_DATA_PATH = "providers/linemarkers"

@TestMetadata(TEST_DATA_PATH)
class ControllerRunLineMarkerProviderTest : ExplytKotlinLightTestCase() {

    override fun getTestDataPath(): String =
        super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springCloud_4_1_3
    )

    fun testController() {
        myFixture.configureByFile("ProductController.kt")
        myFixture.doHighlighting()

        val lineMarkers = myFixture.findAllGutters()
            .filter { it.icon == AllIcons.RunConfigurations.TestState.Run }

        assertEquals(2, lineMarkers.size)

        assertEquals(
            setOf("ProductController", "getProduct"),
            lineMarkers
                .mapNotNull { it as? LineMarkerInfo.LineMarkerGutterIconRenderer<*> }
                .mapNotNullTo(mutableSetOf()) { it.lineMarkerInfo.element?.text }
        )
    }

    fun testFeignClient() {
        myFixture.configureByFile("ProductClient.kt")
        myFixture.doHighlighting()

        val lineMarkers = myFixture.findAllGutters()
            .filter { it.icon == AllIcons.RunConfigurations.TestState.Run }

        assertEquals(2, lineMarkers.size)

        assertEquals(
            setOf("ProductClient", "getProduct"),
            lineMarkers
                .mapNotNull { it as? LineMarkerInfo.LineMarkerGutterIconRenderer<*> }
                .mapNotNullTo(mutableSetOf()) { it.lineMarkerInfo.element?.text }
        )
    }

}