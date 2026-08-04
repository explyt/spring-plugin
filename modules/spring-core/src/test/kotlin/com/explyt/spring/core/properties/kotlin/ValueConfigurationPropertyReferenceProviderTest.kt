/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.kotlin

import com.explyt.spring.core.properties.providers.ValueConfigurationPropertyReferenceProvider
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.system.measureTimeMillis

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

    /**
     * A SpEL default such as `:#{null}` introduces a nested brace group inside the placeholder.
     * The key must still be extracted, otherwise no property reference is created and the property
     * is reported as unresolved in properties/YAML files.
     */
    fun testValueWithSpelDefaultResolvesReference() {
        assertPlaceholderReference("${'$'}{my.prop<caret>erty:#{null}}")
    }

    /** A nested placeholder default is also brace-delimited and must not break key extraction. */
    fun testValueWithNestedPlaceholderDefaultResolvesReference() {
        assertPlaceholderReference("${'$'}{my.prop<caret>erty:${'$'}{fallback.key}}")
    }

    /** Plain and empty defaults keep working. */
    fun testValueWithPlainDefaultsResolveReference() {
        assertPlaceholderReference("${'$'}{my.prop<caret>erty:}")
        assertPlaceholderReference("${'$'}{my.prop<caret>erty:someDefault}")
        assertPlaceholderReference("${'$'}{my.prop<caret>erty:VeaiGPT/}")
    }

    /** Each placeholder in a composite value contributes its own key. */
    fun testValueWithSeveralPlaceholdersResolvesEveryKey() {
        assertExtractedKeys(
            "${'$'}{first.key} between ${'$'}{second.key:#{null}}",
            "first.key", "second.key"
        )
    }

    /** A pure SpEL value holds no property placeholder, so no property key may be extracted. */
    fun testPureSpelValueHasNoPropertyKey() {
        assertExtractedKeys("#{someBean.someProperty}")
    }

    /** An unterminated placeholder must not be silently treated as a resolvable key. */
    fun testUnbalancedPlaceholderHasNoPropertyKey() {
        assertExtractedKeys("${'$'}{my.property:#{null}")
    }

    /** The extracted key must never include the default value. */
    fun testExtractedKeyExcludesDefaultValue() {
        assertExtractedKeys("${'$'}{my.property:#{null}}", "my.property")
        assertExtractedKeys("${'$'}{my.property:${'$'}{fallback.key}}", "my.property")
        assertExtractedKeys("${'$'}{my.property:someDefault}", "my.property")
        assertExtractedKeys("${'$'}{my.property:}", "my.property")
        assertExtractedKeys("${'$'}{my.property}", "my.property")
    }

    /**
     * Guards the placeholder pattern against catastrophic backtracking: an ambiguous alternation
     * over brace groups takes exponential time on this input (~350 ms at 22 groups and minutes
     * beyond that), which would freeze highlighting while the user edits an annotation.
     */
    fun testDeeplyRepeatedBraceGroupsMatchInLinearTime() {
        val value = "${'$'}{my.property:" + "${'$'}{a}".repeat(200)
        val elapsedMs = measureTimeMillis {
            ValueConfigurationPropertyReferenceProvider.PROPERTIES_PATTERN.matcher(value).find()
        }
        assertTrue(
            "Placeholder matching took $elapsedMs ms, expected far below the 2000 ms guard",
            elapsedMs < 2000
        )
    }

    /**
     * Configures a component whose `@Value` argument is the given placeholder and asserts that a
     * reference is offered at the caret.
     *
     * The fixture is Kotlin source, so every `$` must be escaped: an unescaped `${...}` would be
     * parsed as a Kotlin string template instead of a literal placeholder, and the test would
     * exercise template resolution rather than the provider under test.
     */
    private fun assertPlaceholderReference(placeholder: String) {
        val escapedPlaceholder = placeholder.replace("$", "\\$")
        myFixture.configureByText(
            "TestComponent.kt",
            """
            import org.springframework.beans.factory.annotation.Value

            class TestComponent {
                @Value("$escapedPlaceholder")
                private val injected: String = ""
            }
            """.trimIndent()
        )
        assertNoParseErrors(placeholder)

        // The caret sits inside the key, so a reference must be offered there. The exact key
        // boundaries are asserted separately in testExtractedKeyExcludesDefaultValue.
        val reference = file.findReferenceAt(myFixture.caretOffset)
        assertNotNull("Expected a reference for placeholder '$placeholder'", reference)
    }

    /** Fails if the generated fixture does not parse, which would silently invalidate the test. */
    private fun assertNoParseErrors(placeholder: String) {
        val error = PsiTreeUtil.findChildOfType(file, PsiErrorElement::class.java)
        assertNull(
            "The fixture for placeholder '$placeholder' does not parse as Kotlin: ${error?.errorDescription}",
            error
        )
    }

    /** Asserts the keys the provider's placeholder pattern extracts from an annotation value. */
    private fun assertExtractedKeys(value: String, vararg expectedKeys: String) {
        val matcher = ValueConfigurationPropertyReferenceProvider.PROPERTIES_PATTERN.matcher(value)
        val actualKeys = generateSequence { if (matcher.find()) matcher.group(1) else null }.toList()

        assertEquals(
            "Unexpected placeholder keys extracted from '$value'",
            expectedKeys.toList(),
            actualKeys
        )
    }
}
