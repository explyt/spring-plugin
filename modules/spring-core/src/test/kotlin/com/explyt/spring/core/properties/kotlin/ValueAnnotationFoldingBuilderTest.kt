/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.TextRange
import junit.framework.TestCase

private const val PLACEHOLDER_START = "\${"

class ValueAnnotationFoldingBuilderTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testCronPlaceholderIsFolded() {
        myFixture.addFileToProject("application.properties", "test.property=valueTestFolding")

        val propertyString = "\\" + PLACEHOLDER_START + "test.property}"
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

    fun testValueFromProfileLessPropertiesIsFolded() {
        myFixture.addFileToProject("application.properties", "database=h2")

        configureBean("database")

        TestCase.assertEquals("h2", foldedPlaceholderText())
    }

    fun testProfileLessPropertiesWinsOverInactiveProfile() {
        myFixture.addFileToProject("application.properties", "database=h2")
        myFixture.addFileToProject("application-mysql.properties", "database=mysql")

        configureBean("database")

        TestCase.assertEquals("h2", foldedPlaceholderText())
    }

    fun testProfileLessYamlWinsOverInactiveProfile() {
        myFixture.addFileToProject("application.yaml", "database: h2")
        myFixture.addFileToProject("application-mysql.yaml", "database: mysql")

        configureBean("database")

        TestCase.assertEquals("h2", foldedPlaceholderText())
    }

    fun testValueOnlyDefinedInProfileShowsProfileOrigin() {
        myFixture.addFileToProject("application-mysql.properties", "database=mysql")

        configureBean("database")

        TestCase.assertEquals("mysql (profile: mysql)", foldedPlaceholderText())
    }

    fun testActiveProfileWinsOverProfileLessProperties() {
        myFixture.addFileToProject("application.properties", "spring.profiles.active=mysql\ndatabase=h2")
        myFixture.addFileToProject("application-mysql.properties", "database=mysql")

        configureBean("database")

        TestCase.assertEquals("mysql (profile: mysql)", foldedPlaceholderText())
    }

    fun testDefaultValueIsFoldedWhenPropertyIsMissing() {
        myFixture.addFileToProject("application.properties", "database=h2")

        configureBean("missing:fallback")

        TestCase.assertEquals("fallback", foldedPlaceholderText())
    }

    private fun configureBean(placeholder: String) {
        myFixture.configureByText(
            "TestBean.kt", """
            @${SpringCoreClasses.COMPONENT}
            class TestBean {
              @${SpringCoreClasses.VALUE}("\$PLACEHOLDER_START$placeholder}") lateinit var s: String
            }
            """.trimIndent()
        )
        myFixture.doHighlighting()
    }

    private fun foldedPlaceholderText(): String {
        val document = myFixture.editor.document
        val regions = myFixture.editor.foldingModel.allFoldRegions
        val placeholders = regions
            .filter {
                val text = document.getText(TextRange(it.startOffset, it.endOffset))
                text.startsWith("\"") && text.contains(PLACEHOLDER_START)
            }
            .map { it.placeholderText }
        return placeholders.singleOrNull() ?: throw AssertionError(
            "expected a single folded @Value literal, got " + regions.joinToString {
                "[" + it.placeholderText + "] <- [" + document.getText(TextRange(it.startOffset, it.endOffset)) + "]"
            }
        )
    }
}
