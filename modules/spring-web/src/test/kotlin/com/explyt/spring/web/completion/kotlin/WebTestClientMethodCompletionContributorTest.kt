/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.completion.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
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

    fun `test get request to an any-method mapping offers its body type`() {
        addAnyMethodController()

        assertTrue(bodyLookups("get", "/anyMono").any { it.contains("expectBody") })
    }

    fun `test post request to an any-method mapping offers its body type`() {
        addAnyMethodController()

        assertTrue(bodyLookups("post", "/anyMono").any { it.contains("expectBody") })
    }

    fun `test post request to a get mapping offers no body type`() {
        addAnyMethodController()

        assertEquals(emptyList<String>(), bodyLookups("post", "/onlyGetMono"))
    }

    fun `test get request to a get mapping offers its body type`() {
        addAnyMethodController()

        assertTrue(bodyLookups("get", "/onlyGetMono").any { it.contains("expectBody") })
    }

    private fun addAnyMethodController() {
        myFixture.addFileToProject(
            "com/example/AnyController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.*
            import reactor.core.publisher.Mono

            @RestController
            class AnyController {
                @RequestMapping("/anyMono")
                fun anyMono(): Mono<String> = Mono.just("any")

                @GetMapping("/onlyGetMono")
                fun onlyGetMono(): Mono<String> = Mono.just("get")
            }
            """.trimIndent()
        )
        val byPath = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.containingClass?.name == "AnyController" }
            .associate { it.path to it.requestMethods }
        assertEquals(mapOf("/anyMono" to emptyList(), "/onlyGetMono" to listOf("GET")), byPath)
    }

    private fun bodyLookups(verb: String, uri: String): List<String> {
        myFixture.configureByText(
            "AnyControllerTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient

            class AnyControllerTest {
                private var webClient: WebTestClient? = null

                fun justForTest() {
                    webClient?.$verb()?.uri("$uri")?.exchange()
                        ?.eb<caret>
                }
            }
            """.trimIndent()
        )
        return myFixture.completeBasic().orEmpty()
            .mapNotNull { it as? LookupElementBuilder }
            .map { it.toString() }
    }

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