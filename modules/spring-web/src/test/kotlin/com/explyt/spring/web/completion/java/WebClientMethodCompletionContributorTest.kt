/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.completion.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.lookup.LookupElementBuilder

class WebClientMethodCompletionContributorTest : ExplytJavaLightTestCase() {
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
            import org.springframework.web.reactive.function.client.WebClient;
            
            class ProductControllerTest {
                private WebClient webClient;
                
                void justForTest() {
                    webClient.get().uri("/orderMono").retrieve().bt<caret>;
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic().asSequence()
            .mapNotNull { it as? LookupElementBuilder }
            .toList()

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("bodyToMono") })
    }

    fun `test bodyToFlux for Order class`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.java", """
            import org.springframework.web.reactive.function.client.WebClient;
            
            class ProductControllerTest {
                private WebClient webClient;
                
                void justForTest() {
                    webClient.get().uri("/orderFlux").retrieve().bt<caret>;
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic().asSequence()
            .mapNotNull { it as? LookupElementBuilder }
            .toList()

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("bodyToFlux") })
    }

    fun `test bodyToMono for List with wildcard`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.java", """
            import org.springframework.web.reactive.function.client.WebClient;
            
            class ProductControllerTest {
                private WebClient webClient;
                
                void justForTest() {
                    webClient.get().uri("/orderListExtendsUUID").retrieve().bt<caret>;
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic().asSequence()
            .mapNotNull { it as? LookupElementBuilder }
            .toList()

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("bodyToMono") })
    }

    fun `test bodyToFlux for generic type`() {
        myFixture.copyDirectoryToProject("/", "/")
        myFixture.configureByText(
            "OrderControllerTest.java", """
            import org.springframework.web.reactive.function.client.WebClient;
            
            class ProductControllerTest {
                private WebClient webClient;
                
                void justForTest() {
                    webClient.post().uri("/orderGeneric").retrieve().bt<caret>;
                }
            }
        """.trimIndent()
        )

        val lookupDetails = myFixture.completeBasic().asSequence()
            .mapNotNull { it as? LookupElementBuilder }
            .toList()

        assertTrue(lookupDetails.isNotEmpty())
        assertTrue(lookupDetails.any { it.toString().contains("bodyToFlux") })
    }
}