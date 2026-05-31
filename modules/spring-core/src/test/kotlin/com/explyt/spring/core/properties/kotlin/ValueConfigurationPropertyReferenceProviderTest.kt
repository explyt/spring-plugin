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

package com.explyt.spring.core.properties.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary

class ValueConfigurationPropertyReferenceProviderTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    /**
     * Regression for issue #236.
     *
     * The placeholder key is taken from a `const`, so the evaluated `@Value` string
     * (`${'$'}{server.timing.minutes-to-next-claim}`) does not appear literally in the host
     * text (`"${'$'}{${'$'}KEY}"`). The provider used to compute `indexOf(...) == -1` and then
     * build `TextRange.from(-1, length)`, throwing
     * `IllegalArgumentException: Invalid range specified: (-1, n)`. It must now skip the
     * unlocatable key instead of crashing.
     */
    fun testValueReferencingConstantKeyDoesNotThrow() {
        myFixture.configureByText(
            "TestComponent.kt",
            """
            import org.springframework.beans.factory.annotation.Value

            const val KEY = "server.timing.minutes-to-next-claim"

            class TestComponent {
                @Value("${'$'}{${'$'}KEY}")
                private val injected: String = ""
            }
            """.trimIndent()
        )

        // Before the fix this threw IllegalArgumentException while computing references.
        myFixture.doHighlighting()
    }

    /** Happy path: a literal placeholder still yields a property reference. */
    fun testValueWithLiteralPlaceholderResolvesReference() {
        myFixture.configureByText(
            "TestComponent.kt",
            """
            import org.springframework.beans.factory.annotation.Value

            class TestComponent {
                @Value("${'$'}{my.prop<caret>erty}")
                private val injected: String = ""
            }
            """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset)
        assertNotNull("Expected a property reference for the literal placeholder", ref)
    }
}
