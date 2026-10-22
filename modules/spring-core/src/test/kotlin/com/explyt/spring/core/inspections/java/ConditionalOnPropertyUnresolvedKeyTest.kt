/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class ConditionalOnPropertyUnresolvedKeyTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java)
        myFixture.addClass(
            """
            @org.springframework.context.annotation.Configuration
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "app.clickhouse")
            public class AppProperties {
                private String database;

                public String getDatabase() { return database; }
                public void setDatabase(String database) { this.database = database; }
            }
            """.trimIndent()
        )
    }

    fun testConditionalOnPropertyKeyDefinedInTwoFilesIsNotReported() {
        @Language("java") val consumer = """
            @org.springframework.stereotype.Component
            @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
                    name = "app.clickhouse.migration.enabled", havingValue = "true", matchIfMissing = true)
            public class MigrationRunner {
            }
        """.trimIndent()
        myFixture.addClass(consumer)

        configureTwoYamlFiles()

        myFixture.testHighlighting("application.yaml")
        myFixture.testHighlighting("application-local.yaml")
    }

    fun testEnvironmentGetPropertyKeyDefinedInTwoFilesIsNotReported() {
        @Language("java") val consumer = """
            public class MigrationSettings {
                private final org.springframework.core.env.Environment environment;

                public MigrationSettings(org.springframework.core.env.Environment environment) {
                    this.environment = environment;
                }

                public boolean enabled() {
                    return Boolean.parseBoolean(environment.getProperty("app.clickhouse.migration.enabled"));
                }
            }
        """.trimIndent()
        myFixture.addClass(consumer)

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
