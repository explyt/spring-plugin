/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
