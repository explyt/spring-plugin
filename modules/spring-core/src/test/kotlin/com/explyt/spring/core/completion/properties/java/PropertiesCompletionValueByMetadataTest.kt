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

package com.explyt.spring.core.completion.properties.java

import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.completion.CompletionType
import junit.framework.TestCase

class PropertiesCompletionValueByMetadataTest : AbstractSpringPropertiesCompletionContributorTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(
            TestLibrary.springBoot_3_1_1,
            TestLibrary.springContext_6_0_7,
            TestLibrary.resilience4j_2_2_0
        )

    fun testCompleteByHintsValues() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText("application.properties", "main.name=<caret>")
        doTest(
            "create",
            "create-drop",
            "none",
            "update",
            "validate"
        )
    }

    fun testCompleteByHintsProvidersClassReference() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText("application.properties", "main.event-listener=log<caret>")
        doTest("org.springframework.boot.context.logging.LoggingApplicationListener")
    }

    fun testCompleteByHintsProvidersHandleAsEnum() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText("application.properties", "main.code-log-level=<caret>")
        doTest(
            "DEBUG",
            "ERROR",
            "FATAL",
            "INFO",
            "OFF",
            "TRACE",
            "WARN"
        )
    }

    fun testCompleteByHintsProvidersHandleAsMimeType() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText("application.properties", "main.code-log-level=<caret>")
        doTest(
            "DEBUG",
            "ERROR",
            "FATAL",
            "INFO",
            "OFF",
            "TRACE",
            "WARN"
        )
    }

    fun testCompleteByHintsProvidersSpringBeanReference() {
        myFixture.copyFileToProject("FooBeanComponent.java")
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText("application.properties", "main.foo-bean-component=<caret>")
        doTest("fooBeanComponent")
    }

    fun testCompleteByHintsMimeType() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText("application.properties", "main.code-mime-type-additional=html<caret>")
        doTest(
            "application/vnd.dtg.local.html",
            "application/vnd.sealedmedia.softseal.html",
            "text/html",
            "application/vnd.ms-htmlhelp",
            "application/vnd.oipf.dae.xhtml+xml",
            "application/vnd.pwg-xhtml-print+xml",
            "application/xhtml+xml"
        )
    }

    fun testCompleteByEnum() {
        myFixture.copyFileToProject("WeekEnum.java")
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.properties",
            "main.enum-value-additional=SD<caret>"
        )
        doTest(
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
        )
    }

    fun testCompleteByCharset() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.properties",
            "main.charset-additional=UTF<caret>"
        )
        doTest(
            "UTF-8",
            "UTF-16",
            "UTF-16BE",
            "UTF-16LE",
            "UTF-32",
            "UTF-32BE",
            "UTF-32LE",
            "X-UTF-32BE-BOM",
            "X-UTF-32LE-BOM"
        )
    }

    fun testCompleteByLocale() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.properties",
            "main.locale-additional=ru<caret>"
        )
        doTest(
            "ru",
            "ru_BY",
            "ru_KG",
            "ru_KZ",
            "ru_MD",
            "ru_RU",
            "ru_UA",
            "ce_RU",
            "cv_RU",
            "mdf_RU",
            "os_RU",
            "sah_RU",
            "tt_RU"
        )
    }

    fun testCompleteByResource() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.properties",
            "main.code-resource-additional=<caret>"
        )
        doTest(
            "classpath*:", "classpath:", "file:", "http:", "application.properties", "META-INF"
        )
    }

    fun testCompleteByHintsProvidersValuesForMapKeys() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.properties",
            "main.context-additional.<caret>"
        )
        doTest(
            "main.context-additional.sample1", "main.context-additional.sample2"
        )
    }

    fun testCompleteByHintsProvidersValuesForMapValues() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.properties",
            "main.context-additional.sample2=<caret>"
        )
        doTest(
            "1", "2"
        )
    }

    fun testCompleteKeyMap() {
        myFixture.configureByText(
            "application.properties",
            "resilience4j.ratelimiter.<caret>"
        )
        doTest(
            "resilience4j.ratelimiter.configs",
            "resilience4j.ratelimiter.instances",
            "resilience4j.ratelimiter.limiters",
            "resilience4j.ratelimiter.metrics.enabled",
            "resilience4j.ratelimiter.metrics.legacy.enabled",
            "resilience4j.ratelimiter.rate-limiter-aspect-order",
            "resilience4j.ratelimiter.tags"
        )
    }

    fun testCompleteKeyMapKey() {
        myFixture.configureByText(
            "application.properties",
            "resilience4j.ratelimiter.instances.<caret>"
        )
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        TestCase.assertEquals(lookupElementStrings?.size, 0)
    }

    fun _testCompleteKeyMapValue() {
        myFixture.configureByText(
            "application.properties",
            "resilience4j.ratelimiter.instances.test.<caret>"
        )
        doTest(
            "limit-for-period" // и еще
        )
    }
}