/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringConfigurationPropertiesContextInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringConfigurationPropertiesContextInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringConfigurationPropertiesContextInspection::class.java)
    }

    fun testNotInContext() {
        myFixture.configureByText(
            "ConfigProperties.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;

            <warning>@ConfigurationProperties(prefix = "abc")</warning>
            public class ConfigProperties { }
            """.trimIndent()
        )
        myFixture.testHighlighting("ConfigProperties.java")
    }

    fun testInContextConfiguration() {
        myFixture.configureByText(
            "ConfigProperties.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;

            @${SpringCoreClasses.CONFIGURATION}
            @ConfigurationProperties(prefix = "abc")
            public class ConfigProperties { }
            """.trimIndent()
        )
        myFixture.testHighlighting("ConfigProperties.java")
    }

    fun testInContextEnabled() {
        myFixture.addClass(
            """
            import org.springframework.context.annotation.Configuration;
            import org.springframework.boot.context.properties.EnableConfigurationProperties;
            
            @Configuration 
            @EnableConfigurationProperties(ConfigProperties.class)
            public class EnableConfigurationPropertiesConfig { }
        """.trimIndent()
        )
        myFixture.configureByText(
            "ConfigProperties.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            
            @ConfigurationProperties(prefix = "abc")
            public class ConfigProperties { }
            """.trimIndent()
        )
        myFixture.testHighlighting("ConfigProperties.java")
    }

    fun testNotInContextEnabled() {
        myFixture.addClass(
            """
            import org.springframework.context.annotation.Configuration;
            import org.springframework.boot.context.properties.EnableConfigurationProperties;
            
            @Configuration 
            @EnableConfigurationProperties(String.class)
            public class EnableConfigurationPropertiesConfig { }
        """.trimIndent()
        )
        myFixture.configureByText(
            "ConfigProperties.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            
            <warning>@ConfigurationProperties(prefix = "abc")</warning>
            public class ConfigProperties { }
            """.trimIndent()
        )
        myFixture.testHighlighting("ConfigProperties.java")
    }

    fun testInContextPropertiesScan() {
        myFixture.addClass(
            """
            import org.springframework.context.annotation.Configuration;
            import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
            
            @Configuration 
            @ConfigurationPropertiesScan("")
            public class EnableConfigurationPropertiesConfig { }
        """.trimIndent()
        )
        myFixture.configureByText(
            "ConfigProperties.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            
            @ConfigurationProperties(prefix = "abc")
            public class ConfigProperties { }
            """.trimIndent()
        )
        myFixture.testHighlighting("ConfigProperties.java")
    }

    fun testNotInContextPropertiesScanNotInPackage() {
        myFixture.addClass(
            """            
            import org.springframework.context.annotation.Configuration;
            import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
            
            @Configuration 
            @ConfigurationPropertiesScan("com.example")
            public class EnableConfigurationPropertiesConfig { }
        """.trimIndent()
        )
        myFixture.configureByText(
            "ConfigProperties.java",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;
            
            <warning>@ConfigurationProperties(prefix = "abc")</warning>
            public class ConfigProperties { }
            """.trimIndent()
        )
        myFixture.testHighlighting("ConfigProperties.java")
    }
}
