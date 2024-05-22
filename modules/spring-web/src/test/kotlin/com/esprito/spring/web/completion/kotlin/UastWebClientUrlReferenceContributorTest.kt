package com.esprito.spring.web.completion.kotlin

import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.web.references.EspritoControllerMethodReference
import com.intellij.psi.PsiMember
import junit.framework.TestCase

class UastWebClientUrlReferenceContributorTest : EspritoKotlinLightTestCase() {
    override fun getTestDataPath(): String = "${super.getTestDataPath()}/completion/webClient"

    override val libraries: Array<TestLibrary>
        get() = arrayOf(
            TestLibrary.springWeb_6_0_7,
            TestLibrary.springTest_6_0_7,
            TestLibrary.springBootAutoConfigure_3_1_1,
            TestLibrary.springReactiveWeb_3_1_1
        )

    fun testWebClientPut() {
        myFixture.copyFileToProject("ProductController.kt")
        myFixture.configureByText(
            "ProductControllerTest.kt", """
            import org.springframework.web.reactive.function.client.WebClient;
            
            class ProductControllerTest {
                private var webClient: WebClient? = null
    
                fun justForTest() {
                    webClient.put().uri("/product/{pro<caret>duct-id}")
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

    fun testWebTestClientPut() {
        myFixture.copyFileToProject("ProductController.kt")
        myFixture.configureByText(
            "ProductControllerTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient;
            
            class ProductControllerTest {
                private var webClient: WebTestClient? = null
    
                fun justForTest() {
                    webClient.put().uri("/product/{pro<caret>duct-id}")
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

    fun testWebClientNoPatch() {
        myFixture.copyFileToProject("ProductController.kt")
        myFixture.configureByText(
            "ProductControllerTest.kt", """
            import org.springframework.web.reactive.function.client.WebClient;

            class ProductControllerTest {
                private var webClient: WebClient? = null
                
                fun justForTest() {
                    webClient.patch().uri("/product/{pro<caret>duct-id}")
                }
            }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? EspritoControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(0, multiResolve.size)
    }

    fun testWebTestClientNoPatch() {
        myFixture.copyFileToProject("ProductController.kt")
        myFixture.configureByText(
            "ProductControllerTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient;

            class ProductControllerTest {
                private var webClient: WebTestClient? = null
                
                fun justForTest() {
                    webClient.patch().uri("/product/{pro<caret>duct-id}")
                }
            }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? EspritoControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(0, multiResolve.size)
    }

    fun testWebClientGet() {
        myFixture.copyFileToProject("OrderController.kt")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.web.reactive.function.client.WebClient;

            class OrderControllerTest {
                private var webClient: WebClient? = null
                
                fun justForTest() {
                    webClient.get().uri("/or<caret>der")
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

    fun testWebTestClientGet() {
        myFixture.copyFileToProject("OrderController.kt")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient;

            class OrderControllerTest {
                private var webClient: WebTestClient? = null
                
                fun justForTest() {
                    webClient.get().uri("/or<caret>der")
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