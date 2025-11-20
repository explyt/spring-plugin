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

package com.explyt.spring.web.completion.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.lookup.LookupElementBuilder

class WebTestClientMethodCompletionContributorTest : ExplytKotlinLightTestCase() {
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
            import org.springframework.test.web.reactive.server.WebTestClient
            
            class ProductControllerTest {
                private var webClient: WebTestClient? = null
                
                fun justForTest() {
                    webClient?.get()?.uri("/orderMono")?.exchange()
                        ?.eb<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic().asSequence()
            .mapNotNull { it as? LookupElementBuilder }
            .toList()

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("expectBody") })
    }

    fun `test bodyToMono for implicit mono function`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient
            
            class ProductControllerTest {
                private var webClient: WebTestClient? = null
                
                fun justForTest() {
                    webClient?.post()?.uri("/implicitMono")?.exchange()
                        ?.eb<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic().asSequence()
            .mapNotNull { it as? LookupElementBuilder }
            .toList()

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("expectBody") })
    }

    fun `test awaitBody`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient
            
            class ProductControllerTest {
                private var webClient: WebTestClient? = null
                
                fun justForTest() {
                    webClient?.get()?.uri("/orderMono")?.exchange()
                        ?.eb<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic().asSequence()
            .mapNotNull { it as? LookupElementBuilder }
            .toList()

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("expectBody") })
    }

    fun `test bodyToFlow`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient
            
            class ProductControllerTest {
                private var webClient: WebTestClient? = null
                
                fun justForTest() {
                    webClient?.get()?.uri("/orderFlow")?.exchange()
                        ?.eb<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic().asSequence()
            .mapNotNull { it as? LookupElementBuilder }
            .toList()

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("expectBodyList") })
    }

    fun `test bodyToFlux for Order class`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient
            
            class ProductControllerTest {
                private var webClient: WebTestClient? = null
                
                fun justForTest() {
                    webClient?.get()?.uri("/orderFlux")?.exchange()
                        ?.eb<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic().asSequence()
            .mapNotNull { it as? LookupElementBuilder }
            .toList()

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("expectBodyList") })
    }

    fun `test bodyToMono for List with wildcard`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient
            
            class ProductControllerTest {
                private var webClient: WebTestClient? = null
                
                fun justForTest() {
                    webClient?.get()?.uri("/orderListExtendsUUID")?.exchange()
                        ?.eb<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic().asSequence()
            .mapNotNull { it as? LookupElementBuilder }
            .toList()

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("expectBody") })
    }

    fun `test bodyToFlux for generic type`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient
            
            class ProductControllerTest {
                private var webClient: WebTestClient? = null
                
                fun justForTest() {
                    webClient?.post()?.uri("/orderGeneric")?.exchange()
                        ?.eb<caret>
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic().asSequence()
            .mapNotNull { it as? LookupElementBuilder }
            .toList()

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("expectBodyList") })
    }
}