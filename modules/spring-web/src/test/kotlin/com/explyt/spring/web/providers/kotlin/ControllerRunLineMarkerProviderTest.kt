/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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