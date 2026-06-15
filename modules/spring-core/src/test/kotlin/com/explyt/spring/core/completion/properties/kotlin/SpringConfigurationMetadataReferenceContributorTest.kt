/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties.kotlin

import com.explyt.spring.test.TestLibrary

class SpringConfigurationMetadataReferenceContributorTest : AbstractSpringPropertiesCompletionContributorTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBoot_3_1_1, TestLibrary.springContext_6_0_7)

    fun testHintsNameFromClass() {
        myFixture.copyFileToProject("MainFooProperties.kt")
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

}