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

package com.explyt.spring.core.reference.kotlin

import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference
import junit.framework.TestCase

class AdditionalConfigReferenceContributorTest : ExplytKotlinLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + "reference/json"

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testAdditionalConfigPropertiesType() {
        myFixture.configureByText(
            "additional-spring-configuration-metadata.json",
            """
{
  "properties": [
    {
      "name": "main.name", 
      "type": "java.lang.<caret>String"
    }
  ]
}""".trimIndent()
        )
        val ref = myFixture.getReferenceAtCaretPosition() as? JavaClassReference
        assertNotNull(ref)
    }

    fun testAdditionalConfigPropertiesSourceType() {
        val vf = myFixture.copyFileToProject(
            "MainFooProperties.kt",
            "com/boot/MainFooProperties.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        myFixture.configureByText(
            "additional-spring-configuration-metadata.json",
            """
{
  "properties": [
    {
      "name": "main.max-sessions-per-connection",
      "type": "java.lang.Integer",
      "description": "Maximum number connections ",
      "sourceType": "com.boot.<caret>MainFooProperties",
      "defaultValue": 1
    },
  ]
}""".trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition() as? JavaClassReference
        assertNotNull(ref)
    }

    fun testAdditionalConfigHintsName() {
        val vf = myFixture.copyFileToProject(
            "MainFooProperties.kt",
            "com/boot/MainFooProperties.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        myFixture.configureByText(
            "additional-spring-configuration-metadata.json",
            """
{
  "hints": [
    {
      "name": "<caret>main.event-listener",
      "providers": [
        {
          "name": "class-reference",
          "parameters": {
            "target": "org.springframework.context.ApplicationListener"
          }
        }
      ]
    }
  ]
}
""".trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition() as? ConfigurationPropertyKeyReference
        assertNotNull(ref)
    }

    fun testAdditionalConfigHintsProvidersName() {
        val vf = myFixture.copyFileToProject(
            "MainFooProperties.kt",
            "com/boot/MainFooProperties.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        myFixture.configureByText(
            "additional-spring-configuration-metadata.json",
            """
{
  "hints": [
    {
      "name": "main.event-listener",
      "providers": [
        {
          "name": "<caret>class-reference",
          "parameters": {
            "target": "org.springframework.context.ApplicationListener"
          }
        }
      ]
    }
  ]
}
""".trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants
        assertNotNull(variants)
        TestCase.assertEquals(variants?.size, 6)
    }

    fun testAdditionalConfigHintsProvidersParametersTarget() {
        val vf = myFixture.copyFileToProject(
            "MainFooProperties.kt",
            "com/boot/MainFooProperties.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)

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
            "target": "org.springframework.context.<caret>ApplicationListener"
          }
        }
      ]
    }
  ]
}
""".trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition() as? JavaClassReference
        assertNotNull(ref)
    }

}