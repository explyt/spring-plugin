/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.json

import com.explyt.spring.core.completion.ExplytCompletionJavaLightTestCase

class SpringMetadataJsonSchemaFileProviderTest : ExplytCompletionJavaLightTestCase() {

    override fun getTestDataPath(): String = "testdata/completion/json"

    fun testAllVariants() = doTest {
        fileName = "additional-spring-configuration-metadata.json"
        initSource =
            """
{
  <caret>
}
""".trimIndent()
        expectedLookupElements = setOf(
            "\"groups\"",
            "\"properties\"",
            "\"hints\""
        )

        sourceAfterComplete =
            """
{
  "groups": []
}
""".trimIndent()
    }

    fun testGroups() = doTest {
        fileName = "additional-spring-configuration-metadata.json"
        initSource =
            """
{
  "groups": [
    {
      "n<caret>
    }
  ]
}
""".trimIndent()
        expectedLookupElements = setOf(
            "name", "description"
        )
        sourceAfterComplete =
            """
{
  "groups": [
    {
      "name": ""
    }
  ]
}
""".trimIndent()
    }

    fun testProperties() = doTest {
        fileName = "additional-spring-configuration-metadata.json"
        initSource =
            """
{
  "properties": [
    {
      "ty<caret>
    }
  ]
}
""".trimIndent()
        expectedLookupElements = setOf(
            "type", "sourceType"
        )
        sourceAfterComplete =
            """
{
  "properties": [
    {
      "type": ""
    }
  ]
}
""".trimIndent()
    }

    fun testPropertiesDeprecation() = doTest {
        fileName = "additional-spring-configuration-metadata.json"
        initSource =
            """
{
  "properties": [
    {
      "name": "testProperty",
      "deprecation": {
        "<caret>
      }
    }
  ]
}
""".trimIndent()
        expectedLookupElements = setOf(
            "level", "reason", "replacement", "since"
        )
        sourceAfterComplete =
            """
{
  "properties": [
    {
      "name": "testProperty",
      "deprecation": {
        "level": ""
      }
    }
  ]
}
""".trimIndent()
    }

    fun testPropertiesDeprecationLevel() = doTest {
        fileName = "additional-spring-configuration-metadata.json"
        initSource =
            """
{
  "properties": [
    {
      "name": "testProperty",
      "deprecation": {
        "level": "<caret>"
      }
    }
  ]
}
""".trimIndent()
        expectedLookupElements = setOf(
            "warning", "error"
        )
        sourceAfterComplete =
            """
{
  "properties": [
    {
      "name": "testProperty",
      "deprecation": {
        "level": "error"
      }
    }
  ]
}
""".trimIndent()
    }

    fun testHints() = doTest {
        fileName = "additional-spring-configuration-metadata.json"
        initSource =
            """
{
  "hints": [
    {
      "<caret>
    }
  ]
}
""".trimIndent()
        expectedLookupElements = setOf(
            "name", "providers", "values"
        )
        sourceAfterComplete =
            """
{
  "hints": [
    {
      "name": ""
    }
  ]
}
""".trimIndent()
    }

    fun testHintsValues() = doTest {
        fileName = "additional-spring-configuration-metadata.json"
        initSource =
            """
{
  "hints": [
    {
      "name": "testProperty",
      "values": [
        {
          "<caret>
        }
      ]
    }
  ]
}
""".trimIndent()
        expectedLookupElements = setOf(
            "description", "value"
        )
        sourceAfterComplete =
            """
{
  "hints": [
    {
      "name": "testProperty",
      "values": [
        {
          "description": ""
        }
      ]
    }
  ]
}
""".trimIndent()
    }

    fun testHintsProviders() = doTest {
        fileName = "additional-spring-configuration-metadata.json"
        initSource =
            """
{
  "hints": [
    {
      "name": "testProperty",
      "providers": [
        {
          "<caret>
        }
      ]
    }
  ]
}
""".trimIndent()
        expectedLookupElements = setOf(
            "name", "parameters"
        )
        sourceAfterComplete =
            """
{
  "hints": [
    {
      "name": "testProperty",
      "providers": [
        {
          "name": ""
        }
      ]
    }
  ]
}
""".trimIndent()
    }
}