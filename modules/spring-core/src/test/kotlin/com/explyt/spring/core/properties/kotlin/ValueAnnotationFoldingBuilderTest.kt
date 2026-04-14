/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import junit.framework.TestCase

class ValueAnnotationFoldingBuilderTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testCronPlaceholderIsFolded() {
        myFixture.addFileToProject("application.properties", "test.property=valueTestFolding")

        val propertyString = "\\\${test.property}"
        myFixture.configureByText(
            "TestBean.kt", """            
            
            @${SpringCoreClasses.COMPONENT}
            class TestBean {
              @${SpringCoreClasses.VALUE}("$propertyString") lateinit var s: String                             
            }
            """.trimIndent()
        )

        myFixture.checkHighlighting()
        val foldRegion = myFixture.editor.foldingModel.allFoldRegions
            .find { it.placeholderText == "valueTestFolding" }
        TestCase.assertNotNull(foldRegion)
    }
}