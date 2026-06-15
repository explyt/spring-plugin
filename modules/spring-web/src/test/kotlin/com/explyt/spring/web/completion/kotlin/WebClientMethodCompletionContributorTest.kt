/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.completion.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.lookup.LookupElementBuilder

class WebClientMethodCompletionContributorTest : ExplytKotlinLightTestCase() {
    override fun getTestDataPath(): String = "${super.getTestDataPath()}/completion/bodyToMethod"

    override val libraries: Array<TestLibrary>
        get() = arrayOf(
            TestLibrary.springWeb_6_0_7,
            TestLibrary.springTest_6_0_7,
            TestLibrary.springBootAutoConfigure_3_1_1,
            TestLibrary.springReactiveWeb_3_1_1,
            TestLibrary.kotlin_coroutines_1_7_1
        )

    fun `test bodyToMono for Order class`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.web.reactive.function.client.WebClient
            
            class ProductControllerTest {
                private var webClient: WebClient? = null
                
                fun justForTest() {
                    webClient?.get()?.uri("/orderMono")?.retrieve()
                    ?.bt<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic()
            .mapNotNull { it as? LookupElementBuilder }

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("bodyToMono") })
    }

    fun `test bodyToMono for implicit mono function`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.web.reactive.function.client.WebClient
            
            class ProductControllerTest {
                private var webClient: WebClient? = null
                
                fun justForTest() {
                    webClient?.post()?.uri("/implicitMono")?.retrieve()
                    ?.<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic()
            .mapNotNull { it as? LookupElementBuilder }

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("bodyToMono") })
        assertTrue(lookupDetails.any { it.toString().contains("awaitBody") })
    }

    fun `test awaitBody`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.web.reactive.function.client.WebClient
            
            class ProductControllerTest {
                private var webClient: WebClient? = null
                
                fun justForTest() {
                    webClient?.get()?.uri("/orderMono")?.retrieve()
                    ?.ab<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic()
            .mapNotNull { it as? LookupElementBuilder }

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("awaitBody") })
    }

    fun `test bodyToFlow`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.web.reactive.function.client.WebClient
            
            class ProductControllerTest {
                private var webClient: WebClient? = null
                
                fun justForTest() {
                    webClient?.get()?.uri("/orderFlow")?.retrieve()
                    ?.bt<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic()
            .mapNotNull { it as? LookupElementBuilder }

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("bodyToFlux") })
    }

    fun `test bodyToFlux for Order class`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.web.reactive.function.client.WebClient
            
            class ProductControllerTest {
                private var webClient: WebClient? = null
                
                fun justForTest() {
                    webClient?.get()?.uri("/orderFlux")?.retrieve()
                    ?.bt<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic()
            .mapNotNull { it as? LookupElementBuilder }

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("bodyToFlux") })
    }

    fun `test bodyToMono for List with wildcard`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.web.reactive.function.client.WebClient
            
            class ProductControllerTest {
                private var webClient: WebClient? = null
                
                fun justForTest() {
                    webClient?.get()?.uri("/orderListExtendsUUID")?.retrieve()
                    ?.bt<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic()
            .mapNotNull { it as? LookupElementBuilder }

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("bodyToMono") })
    }

    fun `test bodyToFlux for generic type`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.web.reactive.function.client.WebClient
            
            class ProductControllerTest {
                private var webClient: WebClient? = null
                
                fun justForTest() {
                    webClient?.post()?.uri("/orderGeneric")?.retrieve()
                    ?.bt<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic()
            .mapNotNull { it as? LookupElementBuilder }

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("bodyToFlux") })
    }
}