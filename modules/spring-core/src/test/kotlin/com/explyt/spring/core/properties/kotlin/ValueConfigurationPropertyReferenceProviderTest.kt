/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.kotlin

import com.explyt.spring.core.properties.providers.ValueConfigurationPropertyReferenceProvider
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPolyVariantReference
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

    //region SpEL bean references (issue #44)

    /** A bean reference with one property access is the shape the issue is about. */
    fun testSpelBeanPatternExtractsBeanAndMember() {
        assertExtractedBeanMembers("#{@myProps.cron}", "myProps.cron")
    }

    /** A bare bean reference has no member to read, and must still offer the bean. */
    fun testSpelBeanPatternExtractsABareBeanReference() {
        assertExtractedBeanMembers("#{@myProps}", "myProps")
    }

    /** SpEL tolerates whitespace around the accessor, so the pattern has to as well. */
    fun testSpelBeanPatternToleratesWhitespace() {
        assertExtractedBeanMembers("#{ @myProps . cron }", "myProps.cron")
    }

    /** Every bean named in a composite expression contributes its own pair. */
    fun testSpelBeanPatternExtractsEveryBeanInOneBlock() {
        assertExtractedBeanMembers("#{@first.alpha + '-' + @second.beta}", "first.alpha", "second.beta")
    }

    /** A bean referenced from a placeholder's SpEL default is a real reference too. */
    fun testSpelBeanPatternExtractsFromAPlaceholderDefault() {
        assertExtractedBeanMembers("${'$'}{my.property:#{@fallbackProps.cron}}", "fallbackProps.cron")
    }

    /** `#{null}` is the common SpEL default and names no bean. */
    fun testSpelDefaultWithoutABeanExtractsNothing() {
        assertExtractedBeanMembers("${'$'}{my.property:#{null}}")
    }

    /** An `@` outside a SpEL block is literal text — an e-mail address, not a bean. */
    fun testAtSignOutsideSpelExtractsNothing() {
        assertExtractedBeanMembers("support@example.com")
        assertExtractedBeanMembers("${'$'}{mail.from:support@example.com}")
    }

    /** A SpEL block that reads something other than a bean must not be misread as one. */
    fun testSpelWithoutABeanReferenceExtractsNothing() {
        assertExtractedBeanMembers("#{systemProperties['user.name']}")
        assertExtractedBeanMembers("#{T(java.lang.Math).random()}")
    }

    /**
     * The bean named in a SpEL block must navigate. `@Scheduled` resolves the attribute through
     * `EmbeddedValueResolver`, which evaluates SpEL after placeholder resolution, so this value works at runtime
     * and previously produced no references at all.
     */
    fun testScheduledSpelBeanNameResolvesToTheBean() {
        configureSpelFixture("#{@myPr<caret>ops.cron}")

        val resolved = resolveAtCaret()
        // Exactly one: the fixture declares a second bean (TestComponent), and a name-only lookup that fell back
        // to every candidate — as the injection-point reference does — would return both.
        assertEquals(
            "Expected the SpEL bean name to resolve to MyProps alone",
            listOf("MyProps"),
            resolved.map { (it as? PsiClass)?.name }
        )
    }

    /** The member read from the bean must navigate to the property it reads. */
    fun testScheduledSpelMemberResolvesToTheBeanProperty() {
        configureSpelFixture("#{@myProps.cr<caret>on}")

        val resolved = resolveAtCaret()
        assertTrue(
            "Expected the SpEL member to resolve to the cron getter, got ${resolved.map { (it as? PsiMember)?.name }}",
            resolved.any { it is PsiMethod && it.name == "getCron" }
        )
    }

    /** The same value in `@Value` goes through the same provider, so it must resolve identically. */
    fun testValueSpelMemberResolvesToTheBeanProperty() {
        myFixture.configureByText(
            "TestComponent.kt",
            """
            import org.springframework.beans.factory.annotation.Value
            import org.springframework.stereotype.Component

            @Component("myProps")
            class MyProps {
                val cron: String = "0 0 * * * *"
            }

            @Component
            class TestComponent {
                @Value("#{@myProps.cr<caret>on}")
                private val injected: String = ""
            }
            """.trimIndent()
        )

        val resolved = resolveAtCaret()
        assertTrue(
            "Expected the SpEL member to resolve to the cron getter, got ${resolved.map { (it as? PsiMember)?.name }}",
            resolved.any { it is PsiMethod && it.name == "getCron" }
        )
    }

    /** An unknown bean name yields a reference that resolves to nothing, never an exception. */
    fun testUnknownSpelBeanResolvesToNothing() {
        configureSpelFixture("#{@noSuch<caret>Bean.cron}")

        assertNotNull("Expected a reference even for an unknown bean", file.findReferenceAt(myFixture.caretOffset))
        assertTrue("An unknown bean must not resolve", resolveAtCaret().isEmpty())
        // The reference is still computed for the whole file, which must not throw.
        myFixture.doHighlighting()
    }

    /** Configures a component whose `@Scheduled` cron is the given SpEL expression, plus the bean it names. */
    private fun configureSpelFixture(spel: String) {
        myFixture.configureByText(
            "TestComponent.kt",
            """
            import org.springframework.scheduling.annotation.Scheduled
            import org.springframework.stereotype.Component

            @Component("myProps")
            class MyProps {
                val cron: String = "0 0 * * * *"
            }

            @Component
            class TestComponent {
                @Scheduled(cron = "$spel")
                fun run() {
                }
            }
            """.trimIndent()
        )
    }

    /** The elements the reference under the caret resolves to, poly-variant or not. */
    private fun resolveAtCaret(): List<PsiElement?> {
        val reference = file.findReferenceAt(myFixture.caretOffset)
        assertNotNull("Expected a reference at the caret", reference)
        return when (reference) {
            is PsiPolyVariantReference -> reference.multiResolve(false).map { it.element }
            else -> listOfNotNull(reference?.resolve())
        }
    }

    /**
     * Asserts the bean references the provider's SpEL patterns extract, rendered as `bean` or `bean.member`.
     * Mirrors the provider's own two-stage scan — blocks first, then bean accesses inside one — without needing
     * a PSI fixture, so the pattern boundaries can be pinned down independently of bean resolution.
     */
    private fun assertExtractedBeanMembers(value: String, vararg expected: String) {
        val spelMatcher = ValueConfigurationPropertyReferenceProvider.SPEL_PATTERN.matcher(value)
        val actual = mutableListOf<String>()
        while (spelMatcher.find()) {
            val beanMatcher = ValueConfigurationPropertyReferenceProvider.SPEL_BEAN_PATTERN
                .matcher(spelMatcher.group(1))
            while (beanMatcher.find()) {
                val member = beanMatcher.group(2)
                actual += if (member == null) beanMatcher.group(1) else "${beanMatcher.group(1)}.$member"
            }
        }
        assertEquals("Unexpected SpEL bean references extracted from '$value'", expected.toList(), actual)
    }

    //endregion

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
