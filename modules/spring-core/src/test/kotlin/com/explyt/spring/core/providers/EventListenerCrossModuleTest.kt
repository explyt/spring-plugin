/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.addFromMaven
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IndexingTestUtil
import org.intellij.lang.annotations.Language

/**
 * The listener gutter on a `publishEvent` call searches listeners across the whole project and the publisher
 * module's libraries, not only through the publisher module's dependency closure. Each test puts the publisher and
 * the listeners into two different IDEA modules.
 *
 * Events are plain objects (Spring wraps them in `PayloadApplicationEvent`): an `ApplicationEvent` subclass would
 * also match the framework's own `ApplicationListener<ApplicationEvent>` implementations found in spring-context.
 */
class EventListenerCrossModuleTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testListenerInDependentModuleIsFound() {
        addFileToModule(module, "com/example/events/OrderEvent.java", ORDER_EVENT)
        val publisher = addFileToModule(module, "com/example/events/OrderPublisher.java", ORDER_PUBLISHER)
        val app = addDependentModule("app")
        val listener = addFileToModule(app, "com/example/app/OrderListeners.java", ORDER_LISTENERS)

        assertFalse(
            "the listener must be outside the publisher module's dependency scope, otherwise the test proves nothing",
            GlobalSearchScope.moduleWithDependenciesScope(module).contains(listener.virtualFile)
        )

        assertSameElements(eventListenerGutterTargets(publisher), "onOrder(OrderEvent)")
    }

    fun testPublisherInDependentModuleIsFound() {
        addFileToModule(module, "com/example/events/OrderEvent.java", ORDER_EVENT)
        val listener = addFileToModule(module, "com/example/app/OrderListeners.java", ORDER_LISTENERS)
        val app = addDependentModule("app")
        addFileToModule(app, "com/example/events/OrderPublisher.java", ORDER_PUBLISHER)

        assertSameElements(eventPublisherGutterTargets(listener), "publisher.publishEvent(new OrderEvent(id))")
    }

    fun testListenerInDependencyModuleIsFound() {
        val shared = addDependencyModule("shared")
        addFileToModule(shared, "com/example/events/OrderEvent.java", ORDER_EVENT)
        addFileToModule(shared, "com/example/app/OrderListeners.java", ORDER_LISTENERS)
        val publisher = addFileToModule(module, "com/example/events/OrderPublisher.java", ORDER_PUBLISHER)

        assertSameElements(eventListenerGutterTargets(publisher), "onOrder(OrderEvent)")
    }

    fun testListenerOfAnotherEventInOtherModuleIsNotFound() {
        addFileToModule(module, "com/example/events/OrderEvent.java", ORDER_EVENT)
        addFileToModule(module, "com/example/events/PaymentEvent.java", PAYMENT_EVENT)
        val publisher = addFileToModule(module, "com/example/events/OrderPublisher.java", ORDER_PUBLISHER)
        val app = addDependentModule("app")
        addFileToModule(app, "com/example/app/OrderListeners.java", ORDER_LISTENERS)
        addFileToModule(app, "com/example/app/PaymentListeners.java", PAYMENT_LISTENERS)

        assertSameElements(eventListenerGutterTargets(publisher), "onOrder(OrderEvent)")
    }

    fun testApplicationListenerInDependentModuleIsFound() {
        addFileToModule(module, "com/example/events/ShippedEvent.java", SHIPPED_EVENT)
        val publisher = addFileToModule(module, "com/example/events/ShippedPublisher.java", SHIPPED_PUBLISHER)
        val app = addDependentModule("app")
        addFileToModule(app, "com/example/app/ShippedListener.java", SHIPPED_APPLICATION_LISTENER)

        assertContainsElements(eventListenerGutterTargets(publisher), "onApplicationEvent(ShippedEvent)")
    }

    /**
     * The library is a real compiled jar - spring-boot, whose `ApplicationListener<ApplicationEnvironmentPreparedEvent>`
     * implementations (`AnsiOutputApplicationListener`, `FileEncodingApplicationListener`) are found by the library
     * search. A source-only library root does not work here: its listeners are not found.
     */
    fun testListenersInModuleLibraryAndDependentModuleAreFound() {
        attachSpringBoot(module)
        val publisher = addFileToModule(module, "com/example/events/BootPublisher.java", BOOT_PUBLISHER)
        val app = addDependentModule("app")
        attachSpringBoot(app)
        addFileToModule(app, "com/example/app/AppEnvironmentListener.java", APP_ENVIRONMENT_LISTENER)

        assertContainsElements(
            eventListenerGutterTargets(publisher),
            "onEnvironmentPrepared(ApplicationEnvironmentPreparedEvent)",
            "onApplicationEvent(ApplicationEnvironmentPreparedEvent)"
        )
    }

    private fun attachSpringBoot(target: Module) {
        ModuleRootModificationUtil.updateModel(target) { model ->
            addFromMaven(model, TestLibrary.springBoot_3_1_1.mavenCoordinates, false)
        }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private fun eventListenerGutterTargets(publisherFile: PsiFile): List<String> {
        myFixture.configureFromExistingVirtualFile(publisherFile.virtualFile)
        myFixture.doHighlighting()

        val listenerGutters = myFixture.findAllGutters().filter { it.icon == SpringIcons.EventListener }
        assertEquals("the publishEvent call must carry exactly one event listener gutter", 1, listenerGutters.size)

        return SpringGutterTestUtil.getGutterTargetsStrings(listenerGutters.single())
    }

    private fun eventPublisherGutterTargets(publisherFile: PsiFile): List<String> {
        myFixture.configureFromExistingVirtualFile(publisherFile.virtualFile)
        myFixture.doHighlighting()

        val gutters = myFixture.findAllGutters().filter { it.icon == SpringIcons.EventPublisher }
        assertEquals("the publishEvent call must carry exactly one event listener gutter", 1, gutters.size)

        return SpringGutterTestUtil.getGutterTargetsStrings(gutters.single())
    }


    companion object {

        @Language("JAVA")
        private val ORDER_EVENT = """
            package com.example.events;

            public record OrderEvent(String id) {}
        """.trimIndent()

        @Language("JAVA")
        private val PAYMENT_EVENT = """
            package com.example.events;

            public record PaymentEvent(String id) {}
        """.trimIndent()

        @Language("JAVA")
        private val ORDER_PUBLISHER = """
            package com.example.events;

            import org.springframework.context.ApplicationEventPublisher;
            import org.springframework.stereotype.Component;

            @Component
            public class OrderPublisher {
                private final ApplicationEventPublisher publisher;

                public OrderPublisher(ApplicationEventPublisher publisher) {
                    this.publisher = publisher;
                }

                public void place(String id) {
                    publisher.publishEvent(new OrderEvent(id));
                }
            }
        """.trimIndent()

        @Language("JAVA")
        private val ORDER_LISTENERS = """
            package com.example.app;

            import com.example.events.OrderEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class OrderListeners {
                @EventListener
                public void onOrder(OrderEvent event) {}
            }
        """.trimIndent()

        @Language("JAVA")
        private val PAYMENT_LISTENERS = """
            package com.example.app;

            import com.example.events.PaymentEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class PaymentListeners {
                @EventListener
                public void onPayment(PaymentEvent event) {}
            }
        """.trimIndent()

        @Language("JAVA")
        private val SHIPPED_EVENT = """
            package com.example.events;

            import org.springframework.context.ApplicationEvent;

            public class ShippedEvent extends ApplicationEvent {
                public ShippedEvent(Object source) { super(source); }
            }
        """.trimIndent()

        @Language("JAVA")
        private val SHIPPED_PUBLISHER = """
            package com.example.events;

            import org.springframework.context.ApplicationEventPublisher;
            import org.springframework.stereotype.Component;

            @Component
            public class ShippedPublisher {
                private final ApplicationEventPublisher publisher;

                public ShippedPublisher(ApplicationEventPublisher publisher) {
                    this.publisher = publisher;
                }

                public void ship() {
                    publisher.publishEvent(new ShippedEvent(this));
                }
            }
        """.trimIndent()

        @Language("JAVA")
        private val SHIPPED_APPLICATION_LISTENER = """
            package com.example.app;

            import com.example.events.ShippedEvent;
            import org.springframework.context.ApplicationListener;
            import org.springframework.stereotype.Component;

            @Component
            public class ShippedListener implements ApplicationListener<ShippedEvent> {
                @Override
                public void onApplicationEvent(ShippedEvent event) {}
            }
        """.trimIndent()

        @Language("JAVA")
        private val BOOT_PUBLISHER = """
            package com.example.events;

            import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
            import org.springframework.context.ApplicationEventPublisher;
            import org.springframework.stereotype.Component;

            @Component
            public class BootPublisher {
                private final ApplicationEventPublisher publisher;

                public BootPublisher(ApplicationEventPublisher publisher) {
                    this.publisher = publisher;
                }

                public void prepare() {
                    publisher.publishEvent(new ApplicationEnvironmentPreparedEvent(null, null, null, null));
                }
            }
        """.trimIndent()

        @Language("JAVA")
        private val APP_ENVIRONMENT_LISTENER = """
            package com.example.app;

            import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class AppEnvironmentListener {
                @EventListener
                public void onEnvironmentPrepared(ApplicationEnvironmentPreparedEvent event) {}
            }
        """.trimIndent()
    }
}
