/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.annotator

import com.explyt.spring.core.completion.properties.PropertyType
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.TextRange
import org.junit.Assert.assertEquals

class ScalarMapKeyHighlightingTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + "reference/properties"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.hibernate_6_6_36,
    )

    fun testPropertiesScalarMapKeyUsesMapKeyHighlight() {
        val text = "spring.jpa.properties.hibernate.show_sql=true"
        myFixture.configureByText("application.properties", text)

        assertMapType("spring.jpa.properties.hibernate.show_sql", PropertyType.MAP)
        assertEquals(listOf("hibernate.show_sql"), highlightedText(text))
        assertEquals(
            listOf(
                TextRange(
                    text.indexOf("hibernate.show_sql"),
                    text.indexOf("hibernate.show_sql") + "hibernate.show_sql".length
                )
            ),
            highlightedRanges()
        )
    }

    fun testYamlScalarMapKeyUsesMapKeyHighlight() {
        val text = """
            spring:
              jpa:
                properties:
                  hibernate.show_sql: true
        """.trimIndent()
        myFixture.configureByText("application.yaml", text)

        assertMapType("spring.jpa.properties.hibernate.show_sql", PropertyType.MAP)
        assertEquals(listOf("hibernate.show_sql"), highlightedText(text))
    }

    fun testPojoMemberAfterMapKeyIsNotHighlighted() {
        myFixture.copyFileToProject("BracketMapProperties.java")
        val text = "app.publishers.my-registration.owner-application=x"
        myFixture.configureByText("application.properties", text)

        assertMapType("app.publishers.my-registration.owner-application", PropertyType.MAP)
        assertEquals(listOf("my-registration"), highlightedText(text))
    }

    fun testYamlNestedScalarMapKeyHighlightsEverySegment() {
        val text = """
            spring:
              jpa:
                properties:
                  hibernate:
                    show_sql: true
        """.trimIndent()
        myFixture.configureByText("application.yaml", text)

        assertMapType("spring.jpa.properties.hibernate.show_sql", PropertyType.MAP)
        assertEquals(listOf("hibernate", "show_sql"), highlightedText(text))
    }

    fun testEnumMapKeyIsNotHighlighted() {
        myFixture.copyFileToProject("WeekEnum.java")

        myFixture.copyFileToProject("LabelsProperties.java")
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        val text = "app.labels.team.name=x\napp.values.MONDAY=x"
        myFixture.configureByText("application.properties", text)

        assertMapType("app.labels.team.name", PropertyType.MAP)
        assertMapType("app.values.MONDAY", PropertyType.ENUM_MAP)
        assertEquals(listOf("team.name"), highlightedText(text))
    }

    private fun assertMapType(key: String, expected: PropertyType) {
        val property = PropertyUtil.configurationProperty(module, key)
        assertNotNull("fixture must register the map property for $key", property)
        assertEquals("fixture map-key kind for $key", expected, property!!.propertyType)
    }

    private fun highlightedText(text: String): List<String> = highlightedRanges().map { range ->
        text.substring(range.startOffset, range.endOffset)
    }

    private fun highlightedRanges(): List<TextRange> = myFixture.doHighlighting()
        .filter { it.forcedTextAttributesKey?.externalName == MAP_KEY_HIGHLIGHTER_NAME }
        .map { TextRange(it.startOffset, it.endOffset) }

    private companion object {
        const val MAP_KEY_HIGHLIGHTER_NAME = "EXPLYT_MAP_KEY_HIGHLIGHTER"
    }
}
