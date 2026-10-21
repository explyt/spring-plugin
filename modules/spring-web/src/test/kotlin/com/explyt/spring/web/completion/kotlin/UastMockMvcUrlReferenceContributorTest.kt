/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.completion.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.TestUtil.findTypedReferenceAt
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiMember
import junit.framework.TestCase
import org.jetbrains.kotlin.psi.KtCallExpression

class UastMockMvcUrlReferenceContributorTest : ExplytKotlinLightTestCase() {
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
            "com/example/AnyController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.*

            @RestController
            class AnyController {
                @RequestMapping("/any")
                fun any(): String = "any"

                @GetMapping("/only-get")
                fun onlyGet(): String = "get"

                @PostMapping("/only-post")
                fun onlyPost(): String = "post"
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

    private fun configureRequest(request: String) {
        myFixture.configureByText(
            "AnyControllerTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

            class AnyControllerTest {
                fun justForTest() {
                    MockMvcRequestBuilders.$request
                }
            }
            """.trimIndent()
        )
    }

    private fun urlReference(request: String): ExplytControllerMethodReference {
        configureRequest(request)
        val reference = file.findTypedReferenceAt<ExplytControllerMethodReference>(myFixture.caretOffset)
        assertNotNull("The URL must carry an endpoint reference", reference)
        return reference!!
    }

    private fun resolvedNames(request: String): List<String> =
        urlReference(request).multiResolve(false).mapNotNull { (it.element as? PsiMember)?.name }.sorted()

    private fun variantPaths(request: String): List<String> =
        urlReference(request).variants.mapNotNull { (it as? LookupElement)?.lookupString }.sorted()

    fun testPut() {
        myFixture.copyFileToProject("ProductController.kt")
        myFixture.configureByText(
            "ProductControllerTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            
            class ProductControllerTest {
                fun justForTest() {
                    MockMvcRequestBuilders.put("/product/{pro<caret>duct-id}")
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

    /** A URL literal with a host also carries the platform's web reference, so the endpoint one is picked by type. */
    fun testAbsoluteUrlToThisMachineResolves() {
        myFixture.copyFileToProject("ProductController.kt")
        myFixture.configureByText(
            "ProductControllerTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

            class ProductControllerTest {
                fun justForTest() {
                    MockMvcRequestBuilders.put("http://localhost:8080/product/{pro<caret>duct-id}")
                }
            }
        """.trimIndent()
        )

        val ref = file.findTypedReferenceAt<ExplytControllerMethodReference>(myFixture.caretOffset)
        assertNotNull("The absolute URL must carry an endpoint reference", ref)
        val names = ref!!.multiResolve(true).mapNotNull { (it.element as? PsiMember)?.name }
        assertEquals(listOf("update"), names)
    }

    fun testMultipartFirstArg() {
        myFixture.copyFileToProject("ProductController.kt")
        myFixture.configureByText(
            "ProductControllerTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            
            class ProductControllerTest {
                fun justForTest() {
                    MockMvcRequestBuilders.multipart("/product/{pro<caret>duct-id}")
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
        myFixture.copyFileToProject("ProductController.kt")
        myFixture.configureByText(
            "ProductControllerTest.kt", """
            import org.springframework.http.HttpMethod
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            
            class ProductControllerTest {
                fun justForTest() {
                    MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/product/{pro<caret>duct-id}")
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
        myFixture.copyFileToProject("ProductController.kt")
        myFixture.configureByText(
            "ProductControllerTest.kt", """
            import org.springframework.http.HttpMethod
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            
            class ProductControllerTest {
                fun justForTest() {
                    MockMvcRequestBuilders.request(HttpMethod.PUT, "/product/{pro<caret>duct-id}")
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
        myFixture.copyFileToProject("ProductController.kt")
        myFixture.configureByText(
            "ProductControllerTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            
            class ProductControllerTest {
                fun justForTest() {
                    MockMvcRequestBuilders.patch("/product/{pro<caret>duct-id}")
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
        myFixture.copyFileToProject("OrderController.kt")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            
            class OrderControllerTest {
                fun justForTest() {
                    MockMvcRequestBuilders.get("/or<caret>der")
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

    fun testGetCoRouterNest() {
        myFixture.copyFileToProject("UserRouter.kt")
        myFixture.configureByText(
            "WebClientTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class WebClientTest {
                fun justForTest() {
                    MockMvcRequestBuilders.get("/ap<caret>i/users/id")
                }
            }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolvedElement = (multiResolve[0].element) as? KtCallExpression
        assertNotNull(resolvedElement)
    }

    fun testPostCoRouterNest() {
        myFixture.copyFileToProject("UserRouter.kt")
        myFixture.configureByText(
            "WebClientTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class WebClientTest {
                fun justForTest() {
                    MockMvcRequestBuilders.post("/ap<caret>i/users")
                }
            }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolvedElement = (multiResolve[0].element) as? KtCallExpression
        assertNotNull(resolvedElement)
    }

    fun testDeleteCoRouter() {
        myFixture.copyFileToProject("UserRouter.kt")
        myFixture.configureByText(
            "WebClientTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class WebClientTest {
                fun justForTest() {
                    MockMvcRequestBuilders.delete("/user/i<caret>d")
                }
            }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolvedElement = (multiResolve[0].element) as? KtCallExpression
        assertNotNull(resolvedElement)
    }

    fun testPostCoRouterNotExist() {
        myFixture.copyFileToProject("UserRouter.kt")
        myFixture.configureByText(
            "WebClientTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
            
            class WebClientTest {
                fun justForTest() {
                    MockMvcRequestBuilders.post("/user/i<caret>d")
                }
            }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(0, multiResolve.size)
    }

    fun testTemplate() {
        myFixture.copyFileToProject("ProductController.kt")
        myFixture.configureByText(
            "ProductControllerTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            
            class ProductControllerTest {
                fun justForTest() {
                    MockMvcRequestBuilders.put("/product/someProdu<caret>ctId")
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
        myFixture.copyFileToProject("ProductController.kt")
        myFixture.configureByText(
            "ProductControllerTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            
            class ProductControllerTest {
                fun justForTest() {
                    MockMvcRequestBuilders.put("/prod<caret>uct/" + ID)
                }

                companion object {
                    private const val ID = "someProductId"
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