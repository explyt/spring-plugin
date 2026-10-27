/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.completion.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.TestUtil.findTypedReferenceAt
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.codeInsight.lookup.LookupElement
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

    fun testGetCompletionOffersAnAnyMethodMapping() {
        addAnyMethodController()

        assertEquals(listOf("/any", "/only-get"), variantPaths("get(\"/<caret>\")"))
    }

    fun testPostResolvesToAnAnyMethodMapping() {
        addAnyMethodController()

        assertEquals(listOf("any"), resolvedNames("post(\"/<caret>any\")"))
    }

    fun testOptionsResolvesToAnAnyMethodMapping() {
        addAnyMethodController()

        assertEquals(listOf("any"), resolvedNames("options(\"/<caret>any\")"))
    }

    fun testPostDoesNotResolveToAGetMapping() {
        addAnyMethodController()

        assertEquals(emptyList<String>(), resolvedNames("post(\"/only-<caret>get\")"))
    }

    fun testGetResolvesToAGetMapping() {
        addAnyMethodController()

        assertEquals(listOf("onlyGet"), resolvedNames("get(\"/only-<caret>get\")"))
    }

    private fun addAnyMethodController() {
        myFixture.addFileToProject(
            "com/example/AnyController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            public class AnyController {
                @RequestMapping("/any") public String any() { return "any"; }
                @GetMapping("/only-get") public String onlyGet() { return "get"; }
                @PostMapping("/only-post") public String onlyPost() { return "post"; }
            }
            """.trimIndent()
        )
        val byPath = anyControllerEndpoints().associate { it.path to it.requestMethods }
        assertEquals(
            mapOf("/any" to emptyList(), "/only-get" to listOf("GET"), "/only-post" to listOf("POST")),
            byPath
        )
    }

    private fun anyControllerEndpoints(): List<EndpointElement> =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.containingClass?.name == "AnyController" }

    private fun urlReference(request: String): ExplytControllerMethodReference {
        myFixture.configureByText(
            "AnyControllerTest.java", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

            class AnyControllerTest {
                void justForTest() {
                    MockMvcRequestBuilders.$request;
                }
            }
            """.trimIndent()
        )
        val reference = file.findTypedReferenceAt<ExplytControllerMethodReference>(myFixture.caretOffset)
        assertNotNull("The URL must carry an endpoint reference", reference)
        return reference!!
    }

    private fun resolvedNames(request: String): List<String> =
        urlReference(request).multiResolve(false).mapNotNull { (it.element as? PsiMember)?.name }.sorted()

    private fun variantPaths(request: String): List<String> =
        urlReference(request).variants.mapNotNull { (it as? LookupElement)?.lookupString }.sorted()

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