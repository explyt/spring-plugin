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

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

private const val TEST_DATA_PATH = "providers/linemarkers"

@TestMetadata(TEST_DATA_PATH)
class EventListenerLineMarkerProviderTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springTx_6_0_7
    )

    fun testEventListenerLineMarker() {
        val vf = myFixture.copyFileToProject(
            "EventListener.java"
        )

        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allEventGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.EventPublisher || it.icon == SpringIcons.EventListener }
        val gutterTargetString = allEventGutters.map { SpringGutterTestUtil.getGutterTargetsStrings(it) }
        assertTrue(allEventGutters.isNotEmpty())
        for (targets in gutterTargetString) {
            assertTrue(targets.isNotEmpty())
        }
    }

    fun testTransactionalEventListenerLineMarker() {
        myFixture.addClass(
            """
            package com.example;
            public record CustomEvent(String message) {}
            """.trimIndent()
        )

        myFixture.addClass(
            """
            package com.example;
            
            import org.springframework.stereotype.Component;
            import org.springframework.transaction.event.TransactionalEventListener;
            import org.springframework.transaction.event.TransactionPhase;
            
            @Component
            public class TransactionalEventListenerExample {                                
                
                @TransactionalEventListener
                public void handleCustomEventDefault(CustomEvent event) {
                    System.out.println("Handling event with default phase: " + event.getMessage());
                }
            }
            """.trimIndent()
        )

        @Language("java") val string = """
            package com.example;
            
            import org.springframework.context.ApplicationEventPublisher;
            import org.springframework.stereotype.Component;
            
            @Component
            public class EventPublisher {
                private final ApplicationEventPublisher eventPublisher;
                
                public EventPublisher(ApplicationEventPublisher eventPublisher) {
                    this.eventPublisher = eventPublisher;
                }
                
                public void publishEvent(String message) {
                    eventPublisher.publishEvent(new CustomEvent(message));
                }
            }
            """
        myFixture.configureByText(
            "EventPublisher.java",
            string.trimIndent()
        )

        myFixture.doHighlighting()

        val allEventGutters = myFixture.findAllGutters().filter { it.icon == SpringIcons.EventListener }

        assertTrue("Expected to find event gutters", allEventGutters.isNotEmpty())

        // Verify that gutters have targets
        val gutterTargetString = allEventGutters.map { SpringGutterTestUtil.getGutterTargetsStrings(it) }
        for (targets in gutterTargetString) {
            assertTrue("Expected gutter to have targets", targets.isNotEmpty())
        }
    }

}
