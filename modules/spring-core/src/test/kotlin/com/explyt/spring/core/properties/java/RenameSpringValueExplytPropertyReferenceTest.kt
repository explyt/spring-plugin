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

package com.explyt.spring.core.properties.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLKeyValue

class RenameSpringValueExplytPropertyReferenceTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + "properties/rename"

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testRenameProperties() {
        myFixture.configureByFiles(
            "UserHandler.java",
            "application.properties"
        )
        myFixture.configureByText("application-test.properties", "server.timing_new.minutes_to_next_claim=480")

        val element = myFixture.findElementByText("server.timing_new.minutes_to_next_claim", PsiElement::class.java)
        assertNotNull("Property key not found", element)

        myFixture.renameElement(element!!, "server.timing_new.minutes-to-next-claim")

        myFixture.checkResult("server.timing_new.minutes-to-next-claim=480")

        myFixture.checkResult("UserHandler.java", """
            import org.springframework.beans.factory.annotation.Value;

            public class UserHandler {
                @Value("${'$'}{server.timing_new.minutes-to-next-claim:3}")
                private Integer fooFromProperties;
            }
        """.trimIndent(), true)
        myFixture.checkResult("application.properties", "server.timing_new.minutes-to-next-claim=470", true)
    }

    fun testRenameYaml() {
        myFixture.configureByFiles(
            "UserHandler.java",
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

        myFixture.checkResult("UserHandler.java", """
            import org.springframework.beans.factory.annotation.Value;

            public class UserHandler {
                @Value("${'$'}{server.timing_new.minutes-to-next-claim:3}")
                private Integer fooFromProperties;
            }
        """.trimIndent(), true)
        myFixture.checkResult("application.yaml", """
server:
  timing_new:
    minutes-to-next-claim: 470
        """.trimIndent(), true)
    }

}