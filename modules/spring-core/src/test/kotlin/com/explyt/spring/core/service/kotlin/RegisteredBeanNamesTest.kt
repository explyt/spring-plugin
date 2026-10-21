/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.kotlin

import com.explyt.spring.core.inspections.SpringBeanIncorrectAutowiringInspection
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary

class RegisteredBeanNamesTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBeanIncorrectAutowiringInspection::class.java)
    }

    fun testImportedPlainClassIsNamedByQualifiedName() {
        addApplication("@Import(ext.ImportedFoo::class)")
        addExternalClass("ImportedFoo", "")

        assertRegisteredName("ext.ImportedFoo", "ext.ImportedFoo")
    }

    fun testEnabledPropertiesWithPrefixAreNamedByPrefixAndQualifiedName() {
        addApplication("@EnableConfigurationProperties(ext.NamedProps::class)")
        addExternalClass("NamedProps", "@ConfigurationProperties(prefix = \"naming\")")

        assertRegisteredName("naming-ext.NamedProps", "ext.NamedProps")
    }

    fun testEnabledPropertiesWithoutPrefixAreNamedByQualifiedName() {
        addApplication("@EnableConfigurationProperties(ext.PlainProps::class)")
        addExternalClass("PlainProps", "@ConfigurationProperties")

        assertRegisteredName("ext.PlainProps", "ext.PlainProps")
    }

    fun _testAutoConfigurationImportsClassIsNamedByQualifiedName() {
        addApplication("")
        addExternalClass("ListedAutoConfiguration", "@Configuration")
        myFixture.addFileToProject(
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports",
            "ext.ListedAutoConfiguration\n"
        )

        assertRegisteredName("ext.ListedAutoConfiguration", "ext.ListedAutoConfiguration")
    }

    fun testScannedComponentKeepsDefaultName() {
        addApplication("")
        myFixture.addFileToProject(
            "app/ScannedFoo.kt",
            """
            package app
            @org.springframework.stereotype.Component
            class ScannedFoo
            """.trimIndent()
        )

        assertRegisteredName("scannedFoo", "app.ScannedFoo")
    }

    fun testExplicitBeanNameIsKept() {
        addApplication("", "@Bean(name = [\"explicitBar\"]) fun bar() = ext.Bar()")
        addExternalClass("Bar", "")

        assertRegisteredName("explicitBar", "ext.Bar")
    }

    private fun addApplication(registration: String, body: String = "") {
        myFixture.addFileToProject(
            "app/App.kt",
            """
            package app
            import org.springframework.boot.autoconfigure.SpringBootApplication
            import org.springframework.boot.context.properties.EnableConfigurationProperties
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Import
            @SpringBootApplication
            $registration
            open class App {
                $body
            }
            """.trimIndent()
        )
    }

    private fun addExternalClass(simpleName: String, annotation: String) {
        myFixture.addFileToProject(
            "ext/$simpleName.kt",
            """
            package ext
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration
            $annotation
            open class $simpleName
            """.trimIndent()
        )
    }

    private fun assertRegisteredName(beanName: String, injectedType: String) {
        myFixture.configureByText(
            "Consumer.kt",
            """
            package app
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.beans.factory.annotation.Qualifier
            @org.springframework.stereotype.Component
            class Consumer {
                @Autowired
                @Qualifier("$beanName")
                lateinit var injected: $injectedType
            }
            """.trimIndent()
        )
        val beanNames = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .mapTo(sortedSetOf()) { it.name }
        assertTrue("the model names the bean '$beanName', got $beanNames", beanName in beanNames)

        val qualifierErrors = myFixture.doHighlighting().mapNotNull { it.description }
            .filter { it.startsWith("Autowire failed") || it.startsWith("Cannot find bean") }
        assertEquals("@Qualifier(\"$beanName\") resolves", emptyList<String>(), qualifierErrors)
    }
}
