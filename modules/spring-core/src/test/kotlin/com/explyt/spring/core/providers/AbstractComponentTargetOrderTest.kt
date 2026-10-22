/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.psi.search.GlobalSearchScope

class AbstractComponentTargetOrderTest : ExplytJavaLightTestCase() {
    fun testEqualNamesAreOrderedByContainingClassRegardlessOfInputOrder() {
        myFixture.addClass("package candidates; public class ZConsumer { Object foo; }")
        myFixture.addClass("package candidates; public class AConsumer { Object foo; }")
        myFixture.addClass("package candidates; public class FirstFoo {}")
        val firstFoo = findClass("candidates.FirstFoo")
        val aConsumer = findClass("candidates.AConsumer").findFieldByName("foo", false)!!
        val zConsumer = findClass("candidates.ZConsumer").findFieldByName("foo", false)!!
        val expected = listOf(
            "FirstFoo" to "candidates.FirstFoo",
            "foo" to "candidates.AConsumer",
            "foo" to "candidates.ZConsumer"
        )
        val targets = listOf(firstFoo, aConsumer, zConsumer)
        assertEquals(expected, targets.map(::presentation))
        assertEquals(3, targets.map { (it as PsiElement).containingFile.virtualFile.path }.distinct().size)

        assertEquals(
            "Reverse input must not decide the order of equal names",
            expected,
            targets.reversed().sortedInAbstractComponentTargetOrder().map(::presentation)
        )
        assertEquals(
            "Shuffled input must not decide the order of equal names",
            expected,
            listOf(zConsumer, firstFoo, aConsumer).sortedInAbstractComponentTargetOrder().map(::presentation)
        )
    }

    fun testEqualNamesInTheSameClassAndFileAreOrderedByOffset() {
        myFixture.addClass(
            """
            package candidates;
            public class Consumer {
                void first(Object foo) {}
                void second(Object foo) {}
            }
            """.trimIndent()
        )
        val consumer = findClass("candidates.Consumer")
        val first = consumer.findMethodsByName("first", false).single().parameterList.parameters.single()
        val second = consumer.findMethodsByName("second", false).single().parameterList.parameters.single()
        assertEquals("foo", SymbolPresentationUtil.getSymbolPresentableText(first))
        assertEquals("foo", SymbolPresentationUtil.getSymbolPresentableText(second))
        assertSame(consumer, (first.declarationScope as PsiMember).containingClass)
        assertSame(consumer, (second.declarationScope as PsiMember).containingClass)
        assertSame(first.containingFile, second.containingFile)
        assertTrue(first.textOffset < second.textOffset)

        assertEquals(listOf(first, second), listOf(second, first).sortedInAbstractComponentTargetOrder())
    }

    private fun findClass(qualifiedName: String): PsiClass {
        val psiClass = JavaPsiFacade.getInstance(project)
            .findClass(qualifiedName, GlobalSearchScope.projectScope(project))
        assertNotNull("$qualifiedName must resolve", psiClass)
        return psiClass!!
    }

    private fun presentation(target: PsiElement): Pair<String, String?> =
        SymbolPresentationUtil.getSymbolPresentableText(target) to
            ((target as? PsiClass)?.qualifiedName ?: (target as PsiMember).containingClass?.qualifiedName)
}
