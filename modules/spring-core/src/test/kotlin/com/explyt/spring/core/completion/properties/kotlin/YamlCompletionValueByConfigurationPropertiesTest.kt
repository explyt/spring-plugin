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

package com.explyt.spring.core.completion.properties.kotlin

import com.explyt.spring.test.TestLibrary

class YamlCompletionValueByConfigurationPropertiesTest : AbstractSpringPropertiesCompletionContributorTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(
            TestLibrary.springBoot_3_1_1,
            TestLibrary.springContext_6_0_7,
            TestLibrary.slf4j_2_0_7
        )

    fun testCompleteByHintsProvidersSpringBeanReference() {
        myFixture.copyFileToProject("FooBeanComponent.kt")
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

    fun testCompleteByMimeType() {
        myFixture.copyFileToProject("MainFooProperties.kt")
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
        myFixture.copyFileToProject("MainFooProperties.kt")
        myFixture.copyFileToProject("WeekEnum.kt")
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
        myFixture.copyFileToProject("MainFooProperties.kt")
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
        myFixture.copyFileToProject("MainFooProperties.kt")
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
        myFixture.copyFileToProject("MainFooProperties.kt")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  local:
    code-resource: <caret>                
            """.trimIndent()
        )
        doTest(
            "classpath*:", "classpath:", "file:", "http:", "application.yaml", "MainFooProperties.kt"
        )
    }

    fun testCompleteByHintsProvidersValuesForMapKeys() {
        myFixture.copyFileToProject("MainFooProperties.kt")
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
        myFixture.copyFileToProject("MainFooProperties.kt")
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
