/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.LineMarkerInfo.LineMarkerGutterIconRenderer
import junit.framework.TestCase
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.Ignore

private const val TEST_DATA_PATH = "providers/linemarkers"

@TestMetadata(TEST_DATA_PATH)
@Ignore
class ControllerEndpointActionsLineMarkerProviderTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springGraphQl_1_0_4
    )

    fun testYaml() {
        myFixture.configureByFiles("ProductController.kt", "open-api.yaml")
        myFixture.doHighlighting()

        val lineMarkers = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
            .filter { it.tooltipText == "Endpoint Actions" }

        assertEquals(1, lineMarkers.size)

        TestCase.assertEquals(
            "getProduct",
            lineMarkers
                .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }
                .map { it.lineMarkerInfo.element?.text }
                .firstOrNull()
        )

    }

    fun testJson() {
        myFixture.configureByFiles("ProductController.kt", "open-api.json")
        myFixture.doHighlighting()

        val lineMarkers = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
            .filter { it.tooltipText == "Endpoint Actions" }

        assertEquals(1, lineMarkers.size)

        TestCase.assertEquals(
            "getProduct",
            lineMarkers
                .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }
                .map { it.lineMarkerInfo.element?.text }
                .firstOrNull()
        )

    }

    fun testMockMvc() {
        myFixture.configureByFiles("ProductController.kt", "ProductControllerTest.kt")
        myFixture.doHighlighting()

        val lineMarkers = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
            .filter { it.tooltipText == "Endpoint Actions" }

        assertEquals(1, lineMarkers.size)

        TestCase.assertEquals(
            "getProduct",
            lineMarkers
                .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }
                .map { it.lineMarkerInfo.element?.text }
                .firstOrNull()
        )

    }

}