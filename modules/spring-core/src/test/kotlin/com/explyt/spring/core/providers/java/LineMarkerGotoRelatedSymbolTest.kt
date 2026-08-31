/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.providers.EventListenerLineMarkerProvider
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * `Navigate | Related Symbol` reaches a [RelatedItemLineMarkerProvider] through
 * `RelatedItemLineMarkerGotoAdapter`, which calls `collectNavigationMarkers(elements, result, true)`
 * directly and never runs `collectSlowLineMarkers`. These tests pin that alternate entry point so a
 * gate or an early-out cannot be moved into the batch method alone.
 */
class LineMarkerGotoRelatedSymbolTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testEventListenerReachableWithoutCollectSlowLineMarkers() {
        myFixture.addClass(
            """
            package com.example;
            public class CustomEvent {}
            """.trimIndent()
        )
        myFixture.configureByText(
            "EventDemo.java",
            """
            package com.example;

            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.context.ApplicationEventPublisher;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class EventDemo {
                @Autowired
                private ApplicationEventPublisher publisher;

                @EventListener
                public void onCustomEve<caret>nt(CustomEvent event) {
                }

                public void fire() {
                    publisher.publishEvent(new CustomEvent());
                }
            }
            """.trimIndent()
        )

        val markers = collectForNavigation(EventListenerLineMarkerProvider())

        assertTrue(
            "Navigate | Related Symbol must still reach EventListenerLineMarkerProvider",
            markers.isNotEmpty()
        )
    }

    private fun collectForNavigation(
        provider: RelatedItemLineMarkerProvider
    ): List<RelatedItemLineMarkerInfo<*>> {
        val parents: List<PsiElement> = generateSequence(myFixture.file.findElementAt(myFixture.caretOffset)) {
            if (it is PsiFile) null else it.parent
        }.toList()

        val markers = mutableListOf<RelatedItemLineMarkerInfo<*>>()
        provider.collectNavigationMarkers(parents, markers, true)
        return markers
    }
}
