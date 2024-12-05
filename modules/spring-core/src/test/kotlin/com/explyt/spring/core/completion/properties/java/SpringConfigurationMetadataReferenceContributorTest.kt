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

class SpringConfigurationMetadataReferenceContributorTest : AbstractSpringPropertiesCompletionContributorTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBoot_3_1_1, TestLibrary.springContext_6_0_7)

    fun testPropertyType() {
        myFixture.configureByText(
            "additional-spring-configuration-metadata.json",
            """
{
  "properties": [
    {
      "name": "main.value",
      "type": "java.<caret>"
    }
  ]
}
""".trimIndent()
        )
        doTest(
            "io",
            "lang",
            "math",
            "net",
            "nio",
            "security",
            "text",
            "time",
            "util"
        )
    }

    fun testPropertySourceType() {
        myFixture.configureByText(
            "additional-spring-configuration-metadata.json",
            """
{
  "properties": [
    {
      "name": "main.max-sessions-per-connection",
      "type": "java.lang.Integer",
      "description": "Maximum number connections.",
      "sourceType": "org.springframework.core.Nes<caret>",
  ]
}
""".trimIndent()
        )
        doTest(
            "NestedCheckedException",
            "NestedExceptionUtils",
            "NestedRuntimeException",
            "CoroutinesUtils"
        )
    }

    fun testGroupType() {
        myFixture.configureByText(
            "additional-spring-configuration-metadata.json",
            """
{
  "groups": [
    {
        "name": "server",
        "type": "org.s<caret>"
    }
  ]
}
""".trimIndent()
        )
        doTest("springframework", "jetbrains")
    }

    fun testGroupSourceType() {
        myFixture.configureByText(
            "additional-spring-configuration-metadata.json",
            """
{
  "groups": [
    {
      "name": "main.max-sessions-per-connection",
      "type": "java.lang.Integer",
      "sourceType": "org.springframework.core.Nes<caret>",
  ]
}
""".trimIndent()
        )
        doTest(
            "NestedCheckedException",
            "NestedExceptionUtils",
            "NestedRuntimeException",
            "CoroutinesUtils"
        )
    }

    fun testHintsNameFromClass() {
        myFixture.copyFileToProject("MainFooProperties.java")
        myFixture.configureByText(
            "additional-spring-configuration-metadata.json",
            """
{
  "hints": [
    {
      "name": "main.local.<caret>"
    }
}
""".trimIndent()
        )
        doTest(
            "main.local.event-listener",
            "main.local.code-mime-type",
            "main.local.code-charset",
            "main.local.code-locale",
            "main.local.max-sessions-per-connection",
            "main.local.code-resource",
            "main.local.contexts.keys",
            "main.local.contexts.values"
        )
    }

    fun testHintsNameFromMetadata() {
        myFixture.configureByText(
            "additional-spring-configuration-metadata.json",
            """
{
  "hints": [
    {
      "name": "spring.application.<caret>"
    }
}
""".trimIndent()
        )
        doTest("spring.application.index", "spring.application.name")
    }

    fun testHintsProvidersName() {
        myFixture.configureByText(
            "additional-spring-configuration-metadata.json",
            """
{
  "hints": [
    {
      "name": "main.event-listener",
      "providers": [
        {
          "name": "<caret>",
        }
      ]
    }
}
""".trimIndent()
        )
        doTest(
            "any",
            "class-reference",
            "handle-as",
            "logger-name",
            "spring-bean-reference",
            "spring-profile-name"
        )
    }

    fun testHintsProviderParameterTarget() {
        myFixture.configureByText(
            "additional-spring-configuration-metadata.json",
            """
{
  "hints": [
    {
      "name": "main.event-listener",
      "providers": [
        {
          "name": "class-reference",
          "parameters": {
            "target": "org.springframework.context.AppC<caret>"
          }
        }
      ]    }
}
""".trimIndent()
        )
        doTest(
            "ApplicationContext",
            "ApplicationContextAware",
            "ApplicationContextException",
            "ApplicationContextInitializer",
            "ConfigurableApplicationContext"
        )
    }

    fun testHintsProviderParameterTargetByName() {
        myFixture.configureByText(
            "additional-spring-configuration-metadata.json",
            """
{
  "hints": [
    {
      "name": "main.event-listener",
      "providers": [
        {
          "name": "logger-name",
          "parameters": {
            "target": "org.springframework.context.AppC<caret>"
          }
        }
      ]    }
}
""".trimIndent()
        )
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNullOrEmpty(lookupElementStrings)
    }
}