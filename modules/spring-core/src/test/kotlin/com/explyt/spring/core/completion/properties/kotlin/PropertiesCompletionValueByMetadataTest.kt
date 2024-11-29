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
