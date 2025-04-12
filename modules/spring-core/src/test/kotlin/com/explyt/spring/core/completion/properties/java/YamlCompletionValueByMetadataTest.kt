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

class YamlCompletionValueByMetadataTest : AbstractSpringPropertiesCompletionContributorTestCase() {
    override val libraries: Array<TestLibrary> =
        arrayOf(
            TestLibrary.springBoot_3_1_1,
            TestLibrary.springContext_6_0_7,
            TestLibrary.resilience4j_2_2_0
        )

    fun testCompleteByHintsValues() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  name: <caret>
""".trimIndent()
        )

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
        myFixture.configureByText(
            "application.yaml",
            """
main:
  event-listener: log<caret>
""".trimIndent()
        )

        doTest("org.springframework.boot.context.logging.LoggingApplicationListener")
    }

    fun testCompleteByHintsProvidersHandleAs() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  code-log-level: <caret>
""".trimIndent()
        )
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
        myFixture.configureByText(
            "application.yaml",
            """
{                
  main:
    foo-bean-component: <caret>
}
            """.trimIndent()
        )
        doTest("fooBeanComponent")
    }

    fun testCompleteByHintsMimeType() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  code-mime-type-additional: html<caret>                
            """.trimIndent()
        )
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
            "application.yaml",
            """
main:
  enum-value-additional: SD<caret>
            """.trimIndent()
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
            "application.yaml",
            """
main:
  charset-additional: UTF<caret>                
            """.trimIndent()
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
            "application.yaml",
            """
main:
  locale-additional: ru<caret>                
            """.trimIndent()
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
            "application.yaml",
            """
main:
  code-resource-additional: <caret>                
            """.trimIndent()
        )
        doTest(
            "classpath*:", "classpath:", "file:", "http:", "application.yaml", "META-INF"
        )
    }

    fun testCompleteByHintsProvidersValuesForMapKeys() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  context-additional:
    <caret>                
            """.trimIndent()
        )
        doTest(
            "sample1", "sample2"
        )
    }

    fun testCompleteByHintsProvidersValuesForMapValues() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  context-additional:
    sample1: <caret>                
            """.trimIndent()
        )
        doTest(
            "1", "2"
        )
    }

    fun testCompleteKeyMap() {
        myFixture.configureByText(
            "application.yaml",
            """
resilience4j:
  ratelimiter:
    <caret>                
            """.trimIndent()
        )
        doTest(
            "configs",
            "instances",
            "limiters",
            "metrics.enabled",
            "metrics.legacy.enabled",
            "rate-limiter-aspect-order",
            "tags"
        )
    }

    fun testCompleteKeyMapKey() {
        myFixture.configureByText(
            "application.yaml",
            """
resilience4j:
  ratelimiter:
    instances:
      <caret>                
            """.trimIndent()
        )
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        TestCase.assertEquals(lookupElementStrings?.size, 0)
    }

    fun testCompleteKeyMapValueWithColon() {
        myFixture.configureByText(
            "application.yaml",
            """
resilience4j:
  ratelimiter:
    instances:
      test:
        re<caret>:                
            """.trimIndent()
        )
        doTest(
            "resilience4j.ratelimiter.instances.test.register-health-indicator",
            "resilience4j.ratelimiter.instances.test.limit-refresh-period"
        )
    }

    fun _testCompleteKeyMapValue() {
        myFixture.configureByText(
            "application.yaml",
            """
resilience4j:
  ratelimiter:
    instances:
      test:
        re<caret>                
            """.trimIndent()
        )
        doTest(
            "register-health-indicator",
            "limit-refresh-period",
            "limit-refresh-period.nanos",
            "limit-refresh-period.seconds"
        )
    }

    fun _testLoggingLevelAll() {
        myFixture.configureByText(
            "application.yaml",
            """
logging:
  level:
    <caret>
            """.trimMargin()
        )
        doTest(
            "java", "org", "io", "web", "sql", "root"
        )
    }

    fun testLoggingLevelJavaClassOnePackage() {
        myFixture.configureByText(
            "application.yaml",
            """
logging:
  level:
    sql: info
    <caret>
            """.trimMargin()
        )
        doTest(
            "java", "org", "io", "web", "root"
        )
    }

    fun testLoggingLevelJavaClassSecondPackage() {
        myFixture.configureByText(
            "application.yaml",
            """
logging:
  level:
    sql: info
    org.a<caret>
            """.trimMargin()
        )
        doTest(
            "aopalliance", "apache", "jetbrains", "springframework"
        )
    }

}