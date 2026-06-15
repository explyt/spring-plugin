/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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