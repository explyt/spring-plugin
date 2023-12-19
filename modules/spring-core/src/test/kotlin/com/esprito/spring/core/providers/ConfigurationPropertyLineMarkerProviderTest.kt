package com.esprito.spring.core.providers

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
            APPLICATION_PROPERTIES_FILE_NAME,
            "configuration.wrongName=1"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            public class MainPropertiesConfiguration {
    
                private String value;
    
                public String getValue() {
                    return value;
                }
    
                public void set<caret>Value(final String value) {
                    this.value = value;
                }
            }
            """.trimIndent()
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        TestCase.assertEquals(
            listOf<GutterMark>(),
            gutterMarks
        )
    }

    fun testConfigurationProperties() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME,
            "configuration.value=1"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            public class MainPropertiesConfiguration {
    
                private String value;
    
                public String getValue() {
                    return value;
                }
    
                public void set<caret>Value(final String value) {
                    this.value = value;
                }
            }
            """.trimIndent()
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.Property }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.value"),
            gutterTargetsStrings
        )
    }

    fun testNestedInConfigurationProperties() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME,
            "configuration.nested.value=2"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;

            @ConfigurationProperties(prefix="configuration")
            @Configuration
            public class MainPropertiesConfiguration {
                private NestedProperties nested;
                
                public NestedProperties getNested() {
                    return nested;
                }

                public void setNested(final NestedProperties nested) {
                    this.nested = nested;
                }
            }
            
            class NestedProperties {
                private String value;

                public String getValue() {
                    return value;
                }

                public void set<caret>Value(final String value) {
                    this.value = value;
                }
            }
            """.trimIndent()
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.Property }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.nested.value"),
            gutterTargetsStrings
        )
    }

    fun testNestedInBeanProperties() {
        myFixture.addFileToProject(
            APPLICATION_PROPERTIES_FILE_NAME,
            "configuration.value=3"
        )

        myFixture.configureByText(
            "MainPropertiesConfiguration.java",
            """
            import org.springframework.context.annotation.Bean;
            import org.springframework.boot.context.properties.ConfigurationProperties;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class MainPropertiesConfiguration {
                @ConfigurationProperties(prefix="configuration")
                @Bean
                public NestedProperties getNested() {
                    return new NestedProperties();
                }
            }
            
            class NestedProperties {
                private String value;

                public String getValue() {
                    return value;
                }

                public void set<caret>Value(final String value) {
                    this.value = value;
                }
            }
            """.trimIndent()
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.Property }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("configuration.value"),
            gutterTargetsStrings
        )
    }

    companion object {
        const val APPLICATION_PROPERTIES_FILE_NAME = "application.properties"
    }

}