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