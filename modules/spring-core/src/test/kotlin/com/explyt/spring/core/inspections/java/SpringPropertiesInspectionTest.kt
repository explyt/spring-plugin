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

import com.explyt.spring.core.inspections.SpringPropertiesInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

class SpringPropertiesInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringPropertiesInspection::class.java)
    }

    @TestMetadata("properties")
    fun testProperties() = doTest(SpringPropertiesInspection())

    fun testDuplicateProperties() {
        myFixture.configureByText(
            "application.properties",
            """
            <error>test.fooBar</error>=1
            <error>test.foo-bar</error>=2
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testKebabCaseProperties() {
        myFixture.configureByText(
            "application.properties",
            """
            <warning descr="Should be kebab-case">test.fooBar</warning>=1
            test.foo-bar1=2
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testFileDefinitionProperties() {
        @Language("java") val configurationProperty = """
            @org.springframework.context.annotation.Configuration
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt.prop")
            public class ConfigProperties {               
                private String test;                           
                public void setTest(String test) { this.test = test}
                public String getTest() { return this.test}
            } 
        """.trimIndent()
        myFixture.addClass(configurationProperty)
        myFixture.configureByText(
            "application.properties",
            """
            explyt.prop.test=test                
            <warning descr="Cannot resolve key property 'explyt.prop.field'">explyt.prop.field</warning>=test            
            test.foo-bar1=2
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testFileDefinitionPropertiesSubstring() {
        @Language("java") val configurationProperty = """
            @org.springframework.context.annotation.Configuration
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt-prop")
            public class ConfigProperties {               
                private String test;                           
                public void setTest(String test) { this.test = test}
                public String getTest() { return this.test}
            } 
        """.trimIndent()
        myFixture.addClass(configurationProperty)
        myFixture.configureByText(
            "application.properties", """
            explyt.prop.test=test                            
            explyt=test
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testCamelCaseProperties() {
        myFixture.configureByText(
            "application.properties",
            """
            logging.level.org.hibernate.type.descriptor.sql.BasicBinder=debug
            <warning descr="Should be kebab-case">spring.jpa.databasePlatform</warning>=org.hibernate.dialect.PostgreSQLDialect
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }
}
