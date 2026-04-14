/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.jetbrains.yaml.psi.YAMLKeyValue

class RenameSpringValueExplytPropertyReferenceTest : ExplytKotlinLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "properties/rename"

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testRenameProperties() {
        myFixture.configureByFiles(
            "UserHandler.kt",
            "application.properties"
        )
        myFixture.configureByText("application-test.properties", "server.timing_new.minutes_to_next_claim=480")

        val element = myFixture.findElementByText("server.timing_new.minutes_to_next_claim", PsiElement::class.java)
        assertNotNull("Property key not found", element)

        myFixture.renameElement(element!!, "server.timing_new.minutes-to-next-claim")

        myFixture.checkResult("server.timing_new.minutes-to-next-claim=480")

        myFixture.checkResult("UserHandler.kt", UserHandler_after, true)
        myFixture.checkResult("application.properties", "server.timing_new.minutes-to-next-claim=470", true)
    }

    fun testRenameYaml() {
        myFixture.configureByFiles(
            "UserHandler.kt",
            "application.yaml"
        )
        myFixture.configureByText(
            "application-test.yaml",
            """
server:
  timing_new:
    minutes_to_next_claim: 470
""".trimIndent()
        )

        val element = myFixture.findElementByText("minutes_to_next_claim", YAMLKeyValue::class.java)
        assertNotNull("Yaml key not found", element)

        myFixture.renameElement(element!!, "minutes-to-next-claim")

        myFixture.checkResult(
            """
server:
  timing_new:
    minutes-to-next-claim: 470
""".trimIndent()
        )

        myFixture.checkResult("UserHandler.kt", UserHandler_after, true)
        myFixture.checkResult("application.yaml", """
server:
  timing_new:
    minutes-to-next-claim: 470
        """.trimIndent(), true)
    }

}

@Language("kotlin")
private val UserHandler_after = """
import org.springframework.beans.factory.annotation.Value

class UserHandler {
    @Value("\${'$'}{server.timing_new.minutes-to-next-claim}")
    private val minutesToNextClaim: Long = 470L
}
""".trimIndent()