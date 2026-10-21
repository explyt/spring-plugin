/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class ConditionalOnPropertyUnresolvedKeyTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java)
        myFixture.addFileToProject(
            "AppProperties.kt",
            """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ConfigurationProperties(prefix = "app.clickhouse")
            class AppProperties {
                var database: String = ""
            }
            """.trimIndent()
        )
    }

    fun testConditionalOnPropertyKeyDefinedInTwoFilesIsNotReported() {
        @Language("kotlin") val consumer = """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.stereotype.Component

            @Component
            @ConditionalOnProperty(name = ["app.clickhouse.migration.enabled"], havingValue = "true", matchIfMissing = true)
            class MigrationRunner
        """.trimIndent()
        myFixture.addFileToProject("MigrationRunner.kt", consumer)

        configureTwoYamlFiles()

        myFixture.testHighlighting("application.yaml")
        myFixture.testHighlighting("application-local.yaml")
    }

    fun testEnvironmentGetPropertyKeyDefinedInTwoFilesIsNotReported() {
        @Language("kotlin") val consumer = """
            import org.springframework.core.env.Environment

            class MigrationSettings(private val environment: Environment) {
                fun enabled(): Boolean = environment.getProperty("app.clickhouse.migration.enabled").toBoolean()
            }
        """.trimIndent()
        myFixture.addFileToProject("MigrationSettings.kt", consumer)

        configureTwoYamlFiles()

        myFixture.testHighlighting("application.yaml")
        myFixture.testHighlighting("application-local.yaml")
    }

    fun testUnusedKeyIsStillReported() {
        myFixture.configureByText(
            "application.yaml",
            """
            app:
              clickhouse:
                migration:
                  <warning descr="Cannot resolve key property 'app.clickhouse.migration.enabled'">enabled</warning>: false
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    private fun configureTwoYamlFiles() {
        myFixture.configureByText(
            "application.yaml",
            """
            app:
              clickhouse:
                migration:
                  enabled: false
            """.trimIndent()
        )
        myFixture.configureByText(
            "application-local.yaml",
            """
            app:
              clickhouse:
                migration:
                  enabled: true
            """.trimIndent()
        )
    }
}
