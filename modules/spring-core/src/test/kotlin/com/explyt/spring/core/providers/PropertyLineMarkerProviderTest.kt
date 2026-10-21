/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil.getAllBeanGuttersByIcon
import com.explyt.spring.test.util.SpringGutterTestUtil.getGutterTargetString

private const val TEST_DATA_PATH = "testdata/property/"

class PropertyLineMarkerProviderTest : ExplytBaseLightTestCase() {

    override fun getTestDataPath() = TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    fun testPropertiesToMetadataHintsName() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.properties",
            "main.event-listener=1"
        )
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.Hint)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "name" }
        }.size, 1)
    }

    fun testYamlToMetadataHintsName() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  event-listener:                
            """.trimIndent()
        )
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.Hint)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "name" }
        }.size, 1)
    }

    /**
     * `logging.level.keys` is declared by both `additional-spring-configuration-metadata.json` and
     * `spring-configuration-metadata.json` of the same `spring-boot` artifact, which are the same declaration; the
     * gutter must offer it once.
     */
    fun testPropertiesToMetadataHintsMapKeysName() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.properties",
            "logging.level.org.hibernate.SQL=debug"
        )
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.Hint)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        val targets = gutterTargetString.flatMap { gutter -> gutter.filter { it == "name" } }
        assertEquals("the same hint declared twice in one artifact must be offered once: $targets", 1, targets.size)
    }

    fun testYamlToMetadataHintsMapKeysName() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.yaml",
            """
logging:
  level: 
    org.hibernate.SQL: trace
            """.trimIndent()
        )
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.Hint)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        val targets = gutterTargetString.flatMap { gutter -> gutter.filter { it == "name" } }
        assertEquals("the same hint declared twice in one artifact must be offered once: $targets", 1, targets.size)
    }
}