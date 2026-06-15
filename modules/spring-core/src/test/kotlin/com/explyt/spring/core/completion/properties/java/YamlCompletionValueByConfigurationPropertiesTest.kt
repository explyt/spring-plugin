/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties.java

import com.explyt.spring.test.TestLibrary

class YamlCompletionValueByConfigurationPropertiesTest : AbstractSpringPropertiesCompletionContributorTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBoot_3_1_1, TestLibrary.springContext_6_0_7)

    fun testCompleteByMimeType() {
        myFixture.copyFileToProject("MainFooProperties.java")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  local:
    code-mime-type: html<caret>                
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
        myFixture.copyFileToProject("MainFooProperties.java")
        myFixture.copyFileToProject("WeekEnum.java")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  local:
    enum-value: SD<caret>
            """.trimIndent()
        )
        doTest(
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
        )
    }

    fun testCompleteByCharset() {
        myFixture.copyFileToProject("MainFooProperties.java")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  local:
    code-charset: UTF<caret>                
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
        myFixture.copyFileToProject("MainFooProperties.java")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  local:
    code-locale: ru<caret>                
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
        myFixture.copyFileToProject("MainFooProperties.java")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  local:
    code-resource: <caret>                
            """.trimIndent()
        )
        doTest(
            "classpath*:", "classpath:", "file:", "http:", "application.yaml", "MainFooProperties.java"
        )
    }

    fun testCompleteByHintsProvidersValuesForMapKeys() {
        myFixture.copyFileToProject("MainFooProperties.java")
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  local:
    contexts:
      <caret>                
            """.trimIndent()
        )
        doTest(
            "context1", "context2"
        )
    }

    fun testCompleteByHintsProvidersValuesForMapValues() {
        myFixture.copyFileToProject("MainFooProperties.java")
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  local:
    contexts:
      context1: <caret>                
            """.trimIndent()
        )
        doTest(
            "11", "22"
        )
    }

}