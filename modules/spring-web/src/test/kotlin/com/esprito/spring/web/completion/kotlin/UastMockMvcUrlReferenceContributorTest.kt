package com.esprito.spring.web.completion.kotlin

import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.web.references.EspritoControllerMethodReference
import com.intellij.psi.PsiMember
import junit.framework.TestCase

class UastMockMvcUrlReferenceContributorTest : EspritoKotlinLightTestCase() {
    override fun getTestDataPath(): String = "${super.getTestDataPath()}/completion/mockMvc"

    override val libraries: Array<TestLibrary>
        get() = arrayOf(
            TestLibrary.springWeb_6_0_7,
            TestLibrary.springTest_6_0_7,
            TestLibrary.springBootAutoConfigure_3_1_1
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

        val ref = file.findReferenceAt(myFixture.caretOffset) as? EspritoControllerMethodReference
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

        val ref = file.findReferenceAt(myFixture.caretOffset) as? EspritoControllerMethodReference
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

        val ref = file.findReferenceAt(myFixture.caretOffset) as? EspritoControllerMethodReference
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

        val ref = file.findReferenceAt(myFixture.caretOffset) as? EspritoControllerMethodReference
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

        val ref = file.findReferenceAt(myFixture.caretOffset) as? EspritoControllerMethodReference
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

        val ref = file.findReferenceAt(myFixture.caretOffset) as? EspritoControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolvedElement = (multiResolve[0].element) as? PsiMember
        assertNotNull(resolvedElement)
        val memberName = resolvedElement?.name
        val classFqn = resolvedElement?.containingClass?.qualifiedName
        TestCase.assertEquals("OrderController#getOrders", "$classFqn#$memberName")
    }
}