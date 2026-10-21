/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPolyVariantReference

/**
 * The Java half of the SpEL bean references added for issue #44.
 *
 * Worth its own file rather than a case in the Kotlin test: the provider takes its offsets from the raw injection
 * host text, and the host differs between the two languages — a `KtStringTemplateExpression` in Kotlin, a
 * `PsiLiteralExpression` in Java. An off-by-one in that arithmetic resolves the wrong span and is invisible from
 * the Kotlin side alone.
 */
class SpelBeanReferenceTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testSpelBeanNameResolvesToTheBean() {
        configureSpelFixture("#{@myPr<caret>ops.cron}")

        assertEquals(
            "Expected the SpEL bean name to resolve to MyProps alone",
            listOf("MyProps"),
            resolveAtCaret().map { (it as? PsiClass)?.name }
        )
    }

    fun testSpelMemberResolvesToTheBeanProperty() {
        configureSpelFixture("#{@myProps.cr<caret>on}")

        val resolved = resolveAtCaret()
        assertTrue(
            "Expected the SpEL member to resolve to the cron getter, got ${resolved.map { (it as? PsiMember)?.name }}",
            resolved.any { it is PsiMethod && it.name == "getCron" }
        )
    }

    /** The bean reference must cover the name only — not the `@`, and not the member after it. */
    fun testBeanReferenceSpansExactlyTheBeanName() {
        configureSpelFixture("#{@myPr<caret>ops.cron}")

        val reference = file.findReferenceAt(myFixture.caretOffset)
        assertNotNull("Expected a reference at the caret", reference)
        assertEquals(
            "The bean reference must span the bean name exactly",
            "myProps",
            reference!!.rangeInElement.substring(reference.element.text)
        )
    }

    /** And the member reference must cover the member name only, not the dot before it. */
    fun testMemberReferenceSpansExactlyTheMemberName() {
        configureSpelFixture("#{@myProps.cr<caret>on}")

        val reference = file.findReferenceAt(myFixture.caretOffset)
        assertNotNull("Expected a reference at the caret", reference)
        assertEquals(
            "The member reference must span the member name exactly",
            "cron",
            reference!!.rangeInElement.substring(reference.element.text)
        )
    }

    /** A `@Value` host resolves through the same provider as `@Scheduled`. */
    fun testValueSpelMemberResolvesToTheBeanProperty() {
        myFixture.configureByText(
            "TestComponent.java",
            """
            import org.springframework.beans.factory.annotation.Value;
            import org.springframework.stereotype.Component;

            @Component
            public class TestComponent {
                @Value("#{@myProps.cr<caret>on}")
                private String injected;
            }
            """.trimIndent()
        )
        addPropsBean()

        val resolved = resolveAtCaret()
        assertTrue(
            "Expected the SpEL member to resolve to the cron getter, got ${resolved.map { (it as? PsiMember)?.name }}",
            resolved.any { it is PsiMethod && it.name == "getCron" }
        )
    }

    private fun configureSpelFixture(spel: String) {
        myFixture.configureByText(
            "TestComponent.java",
            """
            import org.springframework.scheduling.annotation.Scheduled;
            import org.springframework.stereotype.Component;

            @Component
            public class TestComponent {
                @Scheduled(cron = "$spel")
                public void run() {
                }
            }
            """.trimIndent()
        )
        addPropsBean()
    }

    private fun addPropsBean() {
        myFixture.addClass(
            """
            import org.springframework.stereotype.Component;

            @Component("myProps")
            public class MyProps {
                public String getCron() {
                    return "0 0 * * * *";
                }
            }
            """.trimIndent()
        )
    }

    private fun resolveAtCaret(): List<PsiElement?> {
        val reference = file.findReferenceAt(myFixture.caretOffset)
        assertNotNull("Expected a reference at the caret", reference)
        return when (reference) {
            is PsiPolyVariantReference -> reference.multiResolve(false).map { it.element }
            else -> listOfNotNull(reference?.resolve())
        }
    }
}
