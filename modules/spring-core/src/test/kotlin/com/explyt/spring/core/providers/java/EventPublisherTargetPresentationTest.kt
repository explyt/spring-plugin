/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.intellij.codeInsight.daemon.GutterMark
import org.intellij.lang.annotations.Language

class EventPublisherTargetPresentationTest : ExplytJavaLightTestCase() {

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
            containerText!!.startsWith("AccountTransfer.java:")
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
        @Language("JAVA") val source = """
            package com.example;

            import org.springframework.context.ApplicationEventPublisher;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            public class AccountTransfer {

                public record AccountTransferEvent(String scopeRef, String actorUserId) {}

                @Component
                public static class AccountTransferEventListener {
                    @EventListener
                    public void onAccountTransfer(AccountTransferEvent event) {
                    }
                }

                @Component
                public static class TransferService {
                    private final ApplicationEventPublisher publisher;

                    public TransferService(ApplicationEventPublisher publisher) {
                        this.publisher = publisher;
                    }

                    public void transfer(String scopeRef, String actorUserId) {
                        publisher.publishEvent(
                                new AccountTransferEvent(
                                        scopeRef,
                                        actorUserId
                                )
                        );
                    }
                }
            }
        """.trimIndent()

        myFixture.configureByText("AccountTransfer.java", source)
        myFixture.doHighlighting()
    }

    private fun publisherGutter(): GutterMark {
        val publisherGutters = myFixture.findAllGutters().filter { it.icon == SpringIcons.EventPublisher }
        assertEquals("The fixture must produce exactly one event publisher gutter", 1, publisherGutters.size)
        return publisherGutters.single()
    }
}
