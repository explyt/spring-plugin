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

class UastMockMvcUrlReferenceContributorTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = "${super.getTestDataPath()}/completion/mockMvc"

    override val libraries: Array<TestLibrary>
        get() = arrayOf(
            TestLibrary.springWeb_6_0_7,
            TestLibrary.springTest_6_0_7,
            TestLibrary.springBootAutoConfigure_3_1_1,
            TestLibrary.springReactiveWeb_3_1_1
        )

    fun testPut() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "ProductControllerTest.java", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class ProductControllerTest {
                void justForTest() {
                    MockMvcRequestBuilders.put("/product/{pro<caret>duct-id}");
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
        TestCase.assertEquals("ProductController#update", "$classFqn#$memberName")
    }

    fun testMultipartFirstArg() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "ProductControllerTest.java", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class ProductControllerTest {
                void justForTest() {
                    MockMvcRequestBuilders.multipart("/product/{pro<caret>duct-id}");
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

    fun testMultipartSecondArg() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "ProductControllerTest.java", """
            import org.springframework.http.HttpMethod;
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class ProductControllerTest {
                void justForTest() {
                    MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/product/{pro<caret>duct-id}");
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
        TestCase.assertEquals("ProductController#update", "$classFqn#$memberName")
    }

    fun testRequest() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "ProductControllerTest.java", """
            import org.springframework.http.HttpMethod;
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class ProductControllerTest {
                void justForTest() {
                    MockMvcRequestBuilders.request(HttpMethod.PUT, "/product/{pro<caret>duct-id}");
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
        TestCase.assertEquals("ProductController#update", "$classFqn#$memberName")
    }

    fun testNoPatch() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "ProductControllerTest.java", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class ProductControllerTest {
                void justForTest() {
                    MockMvcRequestBuilders.patch("/product/{pro<caret>duct-id}");
                }
            }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(0, multiResolve.size)
    }

    fun testGet() {
        myFixture.copyFileToProject("OrderController.java")
        myFixture.configureByText(
            "OrderControllerTest.java", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class OrderControllerTest {
                void justForTest() {
                    MockMvcRequestBuilders.get("/or<caret>der");
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
        TestCase.assertEquals("OrderController#getOrders", "$classFqn#$memberName")
    }

    fun testSimpleRouterFunction() {
        myFixture.copyFileToProject("WebClients.java")
        myFixture.configureByText(
            "WebClientTest.java", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class WebClientTest {
                void justForTest() {
                    MockMvcRequestBuilders.get("/us<caret>ers");
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
        TestCase.assertEquals("WebClients#oneRoute", "$classFqn#$memberName")
    }


    fun testDiffPostRouterFunction() {
        myFixture.copyFileToProject("WebClients.java")
        myFixture.configureByText(
            "WebClientTest.java", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class WebClientTest {
                void justForTest() {
                    MockMvcRequestBuilders.post("/users/cus<caret>tomers");
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
        TestCase.assertEquals("WebClients#differentFunction", "$classFqn#$memberName")
    }

    fun testDiffDeleteRouterFunction() {
        myFixture.copyFileToProject("WebClients.java")
        myFixture.configureByText(
            "WebClientTest.java", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class WebClientTest {
                void justForTest() {
                    MockMvcRequestBuilders.delete("/users/{use<caret>rId}");
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
        TestCase.assertEquals("WebClients#differentFunction", "$classFqn#$memberName")
    }

    fun testTemplate() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "ProductControllerTest.java", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class ProductControllerTest {
                void justForTest() {
                    MockMvcRequestBuilders.put("/product/someProdu<caret>ctId");
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
        TestCase.assertEquals("ProductController#update", "$classFqn#$memberName")
    }

    fun testTemplateWithConst() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "ProductControllerTest.java", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            public static final String ID = "someProductId";
            
            class ProductControllerTest {
                void justForTest() {
                    MockMvcRequestBuilders.put("/prod<caret>uct/" + ID);
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
        TestCase.assertEquals("ProductController#update", "$classFqn#$memberName")
    }

}