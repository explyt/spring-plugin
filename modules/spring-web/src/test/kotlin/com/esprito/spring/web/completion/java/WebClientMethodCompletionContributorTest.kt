package com.esprito.spring.web.completion.java

import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation

class WebClientMethodCompletionContributorTest : EspritoJavaLightTestCase() {
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
            .mapTo(mutableSetOf()) { LookupDetails.of(it) }

        assertEquals(
            setOf(
                LookupDetails(
                    "bodyToMono",
                    "(Order.class)",
                    "Mono<Order>"
                )
            ), lookupDetails
        )
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
            .mapTo(mutableSetOf()) { LookupDetails.of(it) }

        assertEquals(
            setOf(
                LookupDetails(
                    "bodyToFlux",
                    "(Order.class)",
                    "Flux<Order>"
                )
            ), lookupDetails
        )
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
            .mapTo(mutableSetOf()) { LookupDetails.of(it) }

        assertEquals(
            setOf(
                LookupDetails(
                    "bodyToMono",
                    "(new ParameterizedTypeReference<List<UUID>>(){})",
                    "Mono<List<? extends UUID>>"
                )
            ), lookupDetails
        )
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
            .mapTo(mutableSetOf()) { LookupDetails.of(it) }

        assertEquals(
            setOf(
                LookupDetails(
                    "bodyToFlux",
                    "(Object.class)",
                    "Flux<T>"
                )
            ), lookupDetails
        )
    }

    data class LookupDetails(val text: String?, val tail: String?, val type: String?) {
        companion object {
            private fun of(presentation: LookupElementPresentation): LookupDetails {
                return LookupDetails(
                    text = presentation.itemText,
                    tail = presentation.tailText,
                    type = presentation.typeText
                )
            }

            fun of(builder: LookupElementBuilder): LookupDetails? {
                val key = builder.get().keys
                    .firstOrNull { it.toString() == "LAST_COMPUTED_PRESENTATION" } //Не нашёл адекватного способа вытащить значение из билдера
                    ?: return null
                val presentation = builder.getUserData(key) as? LookupElementPresentation ?: return null

                return of(presentation)
            }
        }
    }
}