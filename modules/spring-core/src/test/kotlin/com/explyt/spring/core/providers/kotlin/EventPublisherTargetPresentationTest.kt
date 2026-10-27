/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.intellij.codeInsight.daemon.GutterMark
import org.intellij.lang.annotations.Language

class EventPublisherTargetPresentationTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testPublisherTargetIsPresentedAsEnclosingMethodWithFileAndLine() {
        configureAccountTransfer()

        val presentations = SpringGutterTestUtil.getGutterTargetPresentations(publisherGutter())
        assertEquals("The fixture must resolve exactly one publish call", 1, presentations.size)

        val presentation = presentations.single()
        assertFalse(
            "Presented text must not be the raw publish call, but was: ${presentation.presentableText}",
            presentation.presentableText.contains("publishEvent") || presentation.presentableText.contains("\n")
        )
        assertEquals("TransferService#transfer", presentation.presentableText)

        val containerText = presentation.containerText
        assertNotNull("Presented container text must name the file and line", containerText)
        assertTrue(
            "Container text must carry the file name and line, but was: $containerText",
            containerText!!.startsWith("AccountTransfer.kt:")
        )
        assertNotNull(
            "Container text must end with a line number, but was: $containerText",
            containerText.substringAfterLast(':').toIntOrNull()
        )
    }

    fun testPopupTitleNamesTheEventType() {
        configureAccountTransfer()

        assertEquals("Choose 'AccountTransferEvent' Publishers", SpringGutterTestUtil.getGutterPopupTitle(publisherGutter()))
    }

    private fun configureAccountTransfer() {
        @Language("kotlin") val source = """
            package com.example

            import org.springframework.context.ApplicationEventPublisher
            import org.springframework.context.event.EventListener
            import org.springframework.stereotype.Component

            data class AccountTransferEvent(val scopeRef: String, val actorUserId: String)

            @Component
            class AccountTransferEventListener {
                @EventListener
                fun onAccountTransfer(event: AccountTransferEvent) {
                }
            }

            @Component
            class TransferService(private val publisher: ApplicationEventPublisher) {
                fun transfer(scopeRef: String, actorUserId: String) {
                    publisher.publishEvent(
                        AccountTransferEvent(
                            scopeRef = scopeRef,
                            actorUserId = actorUserId,
                        )
                    )
                }
            }
        """.trimIndent()

        myFixture.configureByText("AccountTransfer.kt", source)
        myFixture.doHighlighting()
    }

    private fun publisherGutter(): GutterMark {
        val publisherGutters = myFixture.findAllGutters().filter { it.icon == SpringIcons.EventPublisher }
        assertEquals("The fixture must produce exactly one event publisher gutter", 1, publisherGutters.size)
        return publisherGutters.single()
    }
}
