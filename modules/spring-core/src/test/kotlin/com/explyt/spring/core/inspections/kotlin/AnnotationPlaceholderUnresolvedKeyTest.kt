/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

/**
 * Kotlin twin of the java test: the listener annotation is a stub in the real Spring Kafka
 * package, matching how `@KafkaListener(topics = [...], groupId = ...)` consumes placeholders
 * (issue #380).
 */
class AnnotationPlaceholderUnresolvedKeyTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1,
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java)
    }

    fun testPlaceholderInKafkaListenerAttributesCountsAsUsage() {
        @Language("kotlin") val kafkaListener = """
            package org.springframework.kafka.annotation

            annotation class KafkaListener(
                val topics: Array<String> = [],
                val groupId: String = "",
            )
        """.trimIndent()
        myFixture.configureByText("KafkaListener.kt", kafkaListener)

        @Language("kotlin") val configurationProperty = """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ConfigurationProperties(prefix = "app.clickhouse")
            open class ConfigProperties {
                var database: String = ""
            }
        """.trimIndent()
        myFixture.configureByText("ConfigProperties.kt", configurationProperty)

        @Language("kotlin") val listener = """
            import org.springframework.kafka.annotation.KafkaListener

            class ReferralRewardListener {
                @KafkaListener(
                    topics = ["\${'$'}{app.kafka.transactions-topic}"],
                    groupId = "\${'$'}{app.kafka.referral-reward-group-id}",
                )
                fun listen() {
                }
            }
        """.trimIndent()
        myFixture.configureByText("ReferralRewardListener.kt", listener)

        myFixture.configureByText(
            "application.yaml",
            """
app:
  clickhouse:
    database: test
  kafka:
    transactions-topic: transactions_topic
    referral-reward-group-id: referral_group
    <warning descr="Cannot resolve key property 'app.kafka.unknown-prop'">unknown-prop</warning>: unused
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }
}
