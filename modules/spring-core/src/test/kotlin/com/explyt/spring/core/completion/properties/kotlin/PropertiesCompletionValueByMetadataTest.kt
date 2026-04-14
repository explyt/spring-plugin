/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties.kotlin;

import com.explyt.spring.test.TestLibrary

class PropertiesCompletionValueByMetadataTest : AbstractSpringPropertiesCompletionContributorTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(
            TestLibrary.springBoot_3_1_1,
            TestLibrary.springContext_6_0_7,
            TestLibrary.slf4j_2_0_7
        )

    fun testCompleteByHintsProvidersClassReference() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText("application.properties", "main.event-listener=log<caret>")
        doTest("org.springframework.boot.context.logging.LoggingApplicationListener")
    }

    fun testCompleteByHintsProvidersHandleAs() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText("application.properties", "main.code-log-level=<caret>")
        doTest(
            "DEBUG",
            "ERROR",
            "INFO",
            "TRACE",
            "WARN"
        )
    }

    fun testCompleteByHintsProvidersSpringBeanReference() {
        myFixture.copyFileToProject("FooBeanComponent.kt")
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText("application.properties", "main.foo-bean-component=<caret>")
        doTest("fooBeanComponent")
    }

}
