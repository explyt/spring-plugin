package com.esprito.spring.core.reference

import com.esprito.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference
import com.intellij.testFramework.TestDataPath
import junit.framework.TestCase

private const val TEST_DATA_PATH = "testdata/reference/json"

@TestDataPath("\$CONTENT_ROOT/../../${TEST_DATA_PATH}")

class AdditionalConfigReferenceContributorTest : EspritoJavaLightTestCase() {
    override fun getTestDataPath(): String = TEST_DATA_PATH

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
            "MainFooProperties.java",
            "com/boot/MainFooProperties.java"
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
            "MainFooProperties.java",
            "com/boot/MainFooProperties.java"
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
            "MainFooProperties.java",
            "com/boot/MainFooProperties.java"
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
            "MainFooProperties.java",
            "com/boot/MainFooProperties.java"
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