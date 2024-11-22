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

package com.explyt.spring.core.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.providers.java.ConfigurationPropertyLineMarkerProviderTest
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.explyt.spring.test.util.SpringGutterTestUtil.getAllBeanGuttersByIcon
import com.intellij.codeInsight.daemon.GutterMark
import junit.framework.TestCase
import org.intellij.lang.annotations.Language

class ConfigurationPropertyLineMarkerProviderTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    fun testNoGutter() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME, "configuration.wrongName=1"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.kt", """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            open class MainPropertiesConfiguration {
                var val<caret>ue: String? = null
            }
            """.trimIndent()
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        TestCase.assertEquals(
            listOf<GutterMark>(), gutterMarks
        )
    }

    fun testConfigurationProperties() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME, "configuration.value=1"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.kt", """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            open class MainPropertiesConfiguration {
                var val<caret>ue: String? = null
            }
            """.trimIndent()
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.value"), gutterTargetsStrings
        )
    }

    fun testConstructorParameters() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME, "configuration.value=1"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.kt", """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            open class MainPropertiesConfiguration (
                var valu<caret>e: String? = null
            )
            """.trimIndent()
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.value"), gutterTargetsStrings
        )
    }

    fun testNestedInConfigurationProperties() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME, "configuration.nested.value=2"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.kt", """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            open class MainPropertiesConfiguration {
                var nested: NestedProperties? = null
            }
            
            class NestedProperties {
                var <caret>value: String? = null
            }
            """.trimIndent()
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.nested.value"), gutterTargetsStrings
        )
    }

    fun testNestedInBeanProperties() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME, "configuration.value=3"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.kt", """
            import org.springframework.context.annotation.Bean
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration

            @Configuration
            open class MainPropertiesConfiguration {
                @ConfigurationProperties(prefix="configuration")
                @Bean
                fun getNested() = NestedProperties()
            }
            
            class NestedProperties {
                var <caret>value: String? = null
            }
            """.trimIndent()
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.value"), gutterTargetsStrings
        )
    }

    fun testLineMarkerPropertiesInConstructors() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME, """
                configuration.value=1
                configuration.nested.value=2
            """.trimIndent()
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.kt", """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration

            @ConfigurationProperties(prefix="configuration")
            data class MainPropertiesConfiguration (
                var value: Int?,
                var nested: NestedProperties? = null
            )
            
            data class NestedProperties (
                var value: String? = null
            )
            """.trimIndent()
        )

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringSetting }
        val gutterTargetString = allBeanGutters.asSequence()
            .map { SpringGutterTestUtil.getGutterTargetsStrings(it) }
            .filter { it.isNotEmpty() }
            .toList()

        val expectedElements = setOf("configuration.value", "configuration.nested.value")

        assertTrue(allBeanGutters.isNotEmpty())
        assertEquals(expectedElements.toSet(), gutterTargetString.flatten().toSet())
    }

    fun testMapKeyGutter() {
        myFixture.addFileToProject(
            ConfigurationPropertyLineMarkerProviderTest.APPLICATION_PROPERTIES_FILE_NAME,
            """configuration.value.key.one=1
               configuration.value.key-two=2""".trimMargin()
        )

        @Language("kotlin")
        val text = """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration
            import java.util.Map

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            open class MainPropertiesConfiguration {
                var value: Map<String, Strong>? = null
            }
            """
        myFixture.configureByText(
            "MainPropertiesConfiguration.kt",
            text.trimIndent()
        )
        val gutterMarks = myFixture.findAllGutters()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.value.key.one", "configuration.value.key-two"),
            gutterTargetsStrings
        )
    }

    fun testPropertiesGutter() {
        myFixture.addFileToProject(
            ConfigurationPropertyLineMarkerProviderTest.APPLICATION_PROPERTIES_FILE_NAME,
            """configuration.value.key.one=1
               configuration.value.key-two=2""".trimMargin()
        )

        @Language("kotlin")
        val text = """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration
            import java.util.Properties

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            open class MainPropertiesConfiguration {
                var value: Properties? = null
            }
            """
        myFixture.configureByText(
            "MainPropertiesConfiguration.kt",
            text.trimIndent()
        )
        val gutterMarks = myFixture.findAllGutters()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.value.key.one", "configuration.value.key-two"),
            gutterTargetsStrings
        )
    }

    fun testListGutter() {
        myFixture.addFileToProject(
            ConfigurationPropertyLineMarkerProviderTest.APPLICATION_PROPERTIES_FILE_NAME,
            """configuration.value1[0]=1
               configuration.value1[1]=2
               configuration.value2=1,2,3""".trimMargin()
        )

        @Language("kotlin")
        val text = """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            open class MainPropertiesConfiguration {
                var value1: List<String>? = null
                var value2: List<String>? = null
            }
            """
        myFixture.configureByText(
            "MainPropertiesConfiguration.kt",
            text.trimIndent()
        )
        val gutterMarks = myFixture.findAllGutters()
        val gutterMark = gutterMarks.filter { it.icon == SpringIcons.SpringSetting }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = gutterMark.flatMap { SpringGutterTestUtil.getGutterTargetsStrings(it) }
        TestCase.assertEquals(
            setOf("configuration.value1[0]", "configuration.value1[1]", "configuration.value2"),
            gutterTargetsStrings.toSet()
        )
    }

    fun testConfigurationPropertiesBean() {
        myFixture.configureByText(
            "ConfigurationPropertiesBean.kt",
            """
                @org.springframework.boot.context.properties.ConfigurationProperties
                class ConfigurationPropertiesBean
            """.trimIndent()
        )
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        TestCase.assertTrue(allBeanGutters.isNotEmpty())
    }

    companion object {
        const val APPLICATION_PROPERTIES_FILE_NAME = "application.properties"
    }

}