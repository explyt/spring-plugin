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
