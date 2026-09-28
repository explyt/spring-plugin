/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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

    /**
     * Spring calls a listener when the published event is assignable to the type it declares, so a listener of a
     * subtype is not a target of a base event, and one declaring a supertype every event shares (here
     * `java.io.Serializable`) is not a target of an unrelated event.
     */
    fun testListenerTargetsFollowTheDeclaredTypeOnly() {
        @Language("java") val code = """
            public class EventListenerTest {
                private org.springframework.context.ApplicationEventPublisher eventPublisher;

                public void register() {
                    eventPublisher.publishEvent(new ChildEvent());
                    eventPublisher.publishEvent(new BaseEvent());
                }

                @org.springframework.context.event.EventListener
                public void onBase(BaseEvent event) {}

                @org.springframework.context.event.EventListener
                public void onChild(ChildEvent event) {}

                @org.springframework.context.event.EventListener
                public void onOther(OtherEvent event) {}
            }

            class BaseEvent implements java.io.Serializable {}

            class ChildEvent extends BaseEvent {}

            class OtherEvent implements java.io.Serializable {}
            """
        myFixture.configureByText("EventListenerTest.java", code.trimIndent())
        myFixture.doHighlighting()

        val targetsPerPublisher = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.EventListener }
            .map { gutter ->
                SpringGutterTestUtil.getGutterTargetsStrings(gutter).map { it.substringBefore('(') }.sorted()
            }
            .sortedBy { it.size }

        assertEquals(listOf(listOf("onBase"), listOf("onBase", "onChild")), targetsPerPublisher)
    }

    fun testSuperClassEventListenerLineMarker() {
        @Language("java") val code = """  
            public class EventListenerTest {     
                private org.springframework.context.ApplicationEventPublisher eventPublisher;
            
                public void registerUser() {
                    eventPublisher.publishEvent(new Child2Class());
                    eventPublisher.publishEvent(new Child1Class());
                    eventPublisher.publishEvent(new BaseClass());
                }
            
                @org.springframework.context.event.EventListener
                public void test(BaseClass baseClass) {} 
            }
            
            class BaseClass {}
            
            class Child1Class extends BaseClass {}
            
            class Child2Class extends Child1Class {}
            """
        myFixture.configureByText(
            "EventListenerTest.java",
            code.trimIndent()
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
