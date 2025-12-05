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

import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

class SpringYamlInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java)
    }

    @TestMetadata("yaml")
    fun testYaml() = doTest(SpringYamlInspection())

    fun testDuplicateProperties() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
    <error>barBaz</error>: some1
    <error>bar-Baz</error>: some1
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testKebabCaseProperties() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
    <warning descr="Should be kebab-case">barBaz</warning>: some1
    <warning descr="Should be kebab-case">bar-Baz1</warning>: some1
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
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
            "application.yaml",
            """
explyt.prop:
    test: some1
    <warning descr="Cannot resolve key property 'explyt.prop.field'">field</warning>: some1
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
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
            "application.yaml",
            """
explyt.prop:
    test: some1
explyt: some2
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testComplexKebabCaseProperties() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
    <warning descr="Should be kebab-case">barbaz.testProp</warning>: some1    
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testKebabCaseQuickFix() {
        myFixture.configureByText(
            "application.yaml",
            """
fooFoo:
  bar:
    baz: 5   
            """.trimIndent()
        )
        val quickFix = myFixture.getAllQuickFixes().firstOrNull()
        assertNotNull(quickFix)
        myFixture.launchAction(quickFix!!)
        myFixture.checkResult(
            """
foo-foo:
  bar:
    baz: 5 
        """.trimIndent(), true
        )
    }

    fun testKebabCaseQuickFixMiddle() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
  barFoo:
    baz: 5   
            """.trimIndent()
        )
        val quickFix = myFixture.getAllQuickFixes().firstOrNull()
        assertNotNull(quickFix)
        myFixture.launchAction(quickFix!!)
        myFixture.checkResult(
            """
foo:
  bar-foo:
    baz: 5 
        """.trimIndent(), true
        )
    }

    fun testKebabCaseQuickFixEnd() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
  bar:
    bazFoo: 5   
            """.trimIndent()
        )
        val quickFix = myFixture.getAllQuickFixes().firstOrNull()
        assertNotNull(quickFix)
        myFixture.launchAction(quickFix!!)
        myFixture.checkResult(
            """
foo:
  bar:
    baz-foo: 5 
        """.trimIndent(), true
        )
    }
}