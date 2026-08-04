/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

class SpringYamlInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java)
    }

    @TestMetadata("yaml")
    fun testYaml() = doTest(SpringYamlInspection())

    fun testListElementUnderDigitBoundaryKey() {
        @Language("kotlin") val configurationProperty = """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ConfigurationProperties(prefix = "explyt.digit")
            class S3ConfigProperties {
                var s3Logs: S3Logs = S3Logs()

                class S3Logs {
                    var sources: List<Source> = emptyList()
                }

                class Source {
                    var name: String = ""
                }
            }
        """.trimIndent()
        myFixture.addFileToProject("S3ConfigProperties.kt", configurationProperty)
        myFixture.configureByText(
            "application.yaml",
            """
explyt.digit:
  s3-logs:
    sources:
      - name: first
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testListElementUnderNonKebabKey() {
        @Language("kotlin") val configurationProperty = """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ConfigurationProperties(prefix = "explyt.camel")
            class CamelConfigProperties {
                var camelWritten: Holder = Holder()

                class Holder {
                    var items: List<Item> = emptyList()
                }

                class Item {
                    var name: String = ""
                }
            }
        """.trimIndent()
        myFixture.addFileToProject("CamelConfigProperties.kt", configurationProperty)
        myFixture.configureByText(
            "application.yaml",
            """
explyt.camel:
  camelWritten:
    items:
            - <warning descr="Should be kebab-case">name</warning>: first
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    /**
     * A property that only ever appears in a `@Value` placeholder with a SpEL default
     * (`:#{null}`) is still a real, resolvable property. The nested brace group used to break
     * placeholder key extraction, so no reference was created and the key was reported as
     * unresolved even though Spring injects it at runtime.
     */
    fun testKeyUsedOnlyInValueWithSpelDefault() {
        @Language("kotlin") val consumer = """
            import org.springframework.beans.factory.annotation.Value
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.stereotype.Component

            @ConfigurationProperties(prefix = "explyt.placeholder")
            class PlaceholderKnownProperties {
                var known: String = ""
            }

            @Component
            class PlaceholderConsumer(
                @Value("\${'$'}{explyt.placeholder.spel-default:#{null}}") val spelDefault: String?,
                @Value("\${'$'}{explyt.placeholder.plain-default:someDefault}") val plainDefault: String,
            )
        """.trimIndent()
        myFixture.addFileToProject("PlaceholderConsumer.kt", consumer)
        myFixture.configureByText(
            "application.yaml",
            """
explyt.placeholder:
  known: someValue
  spel-default: someValue
  plain-default: someValue
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }
}
