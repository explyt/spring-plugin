/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

/**
 * A `${...}` placeholder in a Spring annotation attribute is a usage of the configuration key,
 * no matter which `org.springframework.*` annotation carries it — Spring resolves embedded values
 * in all of them, not only in `@Value` and `@Scheduled` (issue #380).
 */
class AnnotationPlaceholderUnresolvedKeyTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springWeb_6_0_7,
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java)
    }

    fun testPlaceholderInRequestMappingAttributeCountsAsUsage() {
        @Language("java") val configurationProperty = """
            @org.springframework.context.annotation.Configuration
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "app.clickhouse")
            public class ConfigProperties {
                private String database;
                public void setDatabase(String database) { this.database = database }
                public String getDatabase() { return this.database }
            }
        """.trimIndent()
        myFixture.addClass(configurationProperty)

        @Language("java") val listener = """
            import org.springframework.web.bind.annotation.RequestMapping;

            public class ListenerController {
                @RequestMapping(path = "${'$'}{app.kafka.transactions-topic}")
                public void handle() {
                }
            }
        """.trimIndent()
        myFixture.addClass(listener)

        myFixture.configureByText(
            "application.yaml",
            """
app:
  clickhouse:
    database: test
  kafka:
    transactions-topic: transactions_topic
    <warning descr="Cannot resolve key property 'app.kafka.unknown-prop'">unknown-prop</warning>: unused
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }
}
