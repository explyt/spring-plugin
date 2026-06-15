/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.completion.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.lookup.LookupElementBuilder

class WebTestClientMethodCompletionContributorTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = "${super.getTestDataPath()}/completion/bodyToMethod"

    override val libraries: Array<TestLibrary>
        get() = arrayOf(
            TestLibrary.springWeb_6_0_7,
            TestLibrary.springTest_6_0_7,
            TestLibrary.springBootAutoConfigure_3_1_1,
            TestLibrary.springReactiveWeb_3_1_1
        )

    fun `test bodyToMono for Order class`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.java", """
            import org.springframework.test.web.reactive.server.WebTestClient;
            
            class ProductControllerTest {
                private WebTestClient webClient;
                
                void justForTest() {
                    webClient.get().uri("/orderMono").exchange().eb<caret>;
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic()
            .mapNotNull { it as? LookupElementBuilder }

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("expectBody") })
    }

    fun `test bodyToFlux for Order class`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.java", """
            import org.springframework.test.web.reactive.server.WebTestClient;
            
            class ProductControllerTest {
                private WebTestClient webClient;
                
                void justForTest() {
                    webClient.get().uri("/orderFlux").exchange().eb<caret>;
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic()
            .mapNotNull { it as? LookupElementBuilder }

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("expectBodyList") })
    }

    fun `test bodyToMono for List with wildcard`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.java", """
            import org.springframework.test.web.reactive.server.WebTestClient;
            
            class ProductControllerTest {
                private WebTestClient webClient;
                
                void justForTest() {
                    webClient.get().uri("/orderListExtendsUUID").exchange().eb<caret>;
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic()
            .mapNotNull { it as? LookupElementBuilder }

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("expectBody") })
    }

    fun `test bodyToFlux for generic type`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.java", """
            import org.springframework.test.web.reactive.server.WebTestClient;
            
            class ProductControllerTest {
                private WebTestClient webClient;
                
                void justForTest() {
                    webClient.post().uri("/orderGeneric").exchange().eb<caret>;
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic()
            .mapNotNull { it as? LookupElementBuilder }

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("expectBodyList") })
    }
}