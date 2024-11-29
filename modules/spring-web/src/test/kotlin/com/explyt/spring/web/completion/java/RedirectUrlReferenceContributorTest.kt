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

package com.explyt.spring.web.completion.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.intellij.psi.PsiMember
import junit.framework.TestCase

class RedirectUrlReferenceContributorTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = "${super.getTestDataPath()}/completion/redirect"

    override val libraries: Array<TestLibrary>
        get() = arrayOf(
            TestLibrary.springWeb_6_0_7,
            TestLibrary.springTest_6_0_7,
            TestLibrary.springBootAutoConfigure_3_1_1
        )

    fun testRedirectNotFound() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "ProductControllerTest.java", """
            class ProductControllerTest {
                void justForTest() {
                    "redirect:/unk<caret>nown";
                }
            }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(0, multiResolve.size)

    }

    fun testRedirectOne() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "ProductControllerTest.java", """
            class ProductControllerTest {
                void justForTest() {
                    "redirect:/product/get/i<caret>t";
                }
            }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolvedElement = (multiResolve[0].element) as? PsiMember
        assertNotNull(resolvedElement)
        val memberName = resolvedElement?.name
        val classFqn = resolvedElement?.containingClass?.qualifiedName
        TestCase.assertEquals("ProductController#getProducts", "$classFqn#$memberName")
    }

    fun testRedirectMulti() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "ProductControllerTest.java", """
            class ProductControllerTest {
                void justForTest() {
                    "redirect:/product/{pro<caret>duct-id}";
                }
            }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(2, multiResolve.size)
        val resolvedElements = multiResolve.asSequence()
            .mapNotNull { it.element as? PsiMember }
            .mapTo(mutableSetOf()) { it.name }
        TestCase.assertEquals(resolvedElements, setOf("getProduct", "update"))
    }

    fun testForwardNotFound() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "ProductControllerTest.java", """
            class ProductControllerTest {
                void justForTest() {
                    "forward:/unk<caret>nown";
                }
            }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(0, multiResolve.size)

    }

    fun testForwardOne() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "ProductControllerTest.java", """
            class ProductControllerTest {
                void justForTest() {
                    "forward:/product/get/i<caret>t";
                }
            }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolvedElement = (multiResolve[0].element) as? PsiMember
        assertNotNull(resolvedElement)
        val memberName = resolvedElement?.name
        val classFqn = resolvedElement?.containingClass?.qualifiedName
        TestCase.assertEquals("ProductController#getProducts", "$classFqn#$memberName")
    }

    fun testForwardMulti() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "ProductControllerTest.java", """
            class ProductControllerTest {
                void justForTest() {
                    "forward:/product/{pro<caret>duct-id}";
                }
            }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(2, multiResolve.size)
        val resolvedElements = multiResolve.asSequence()
            .mapNotNull { it.element as? PsiMember }
            .mapTo(mutableSetOf()) { it.name }
        TestCase.assertEquals(resolvedElements, setOf("getProduct", "update"))
    }

}