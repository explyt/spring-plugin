package com.explyt.spring.web.completion.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.references.ExplytControllerMethodReference
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