/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
