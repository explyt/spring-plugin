/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringPropertiesInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringPropertiesInspectionTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringPropertiesInspection::class.java)
    }

    @TestMetadata("properties")
    fun testProperties() = doTest(SpringPropertiesInspection())

    fun testLoggingLevelUppercaseValueIsNotAnError() {
        myFixture.configureByText(
            "application.properties",
            """
            logging.level.root=INFO
            logging.level.org.springframework.kafka=DEBUG
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testHintValueGateUsesValuesHint() {
        myFixture.copyFileToProject(
            "hintValueGate/META-INF/additional-spring-configuration-metadata.json",
            "META-INF/additional-spring-configuration-metadata.json"
        )
        myFixture.configureByText(
            "application.properties",
            """
            explyt.modes.mode=<error descr="Invalid value 'hyper', must be one of [fast, slow]">hyper</error>
            explyt.maponly.beta=anything
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

}
