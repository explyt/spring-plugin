package com.esprito.spring.core.providers.kotlin

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.util.SpringGutterTestUtil
import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.GutterMark
import junit.framework.TestCase

class ConfigurationPropertyLineMarkerProviderTest : EspritoJavaLightTestCase() {

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

    companion object {
        const val APPLICATION_PROPERTIES_FILE_NAME = "application.properties"
    }

}