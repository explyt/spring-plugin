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
import javax.swing.Icon

/**
 * Event gutters search the Spring contexts the module can run in: every module that depends on it, with everything
 * those modules depend on, plus the module's libraries. Each test puts the publisher and the listeners into different
 * IDEA modules. Events are plain objects, which Spring wraps in `PayloadApplicationEvent`.
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

        assertSameElements(gutterTargets(publisher, SpringIcons.EventListener), "onOrder(OrderEvent)")
    }

    fun testPublisherInDependentModuleIsFound() {
        addFileToModule(module, "com/example/events/OrderEvent.java", ORDER_EVENT)
        val listener = addFileToModule(module, "com/example/app/OrderListeners.java", ORDER_LISTENERS)
        val app = addDependentModule("app")
        addFileToModule(app, "com/example/events/OrderPublisher.java", ORDER_PUBLISHER)

        assertSameElements(gutterTargets(listener, SpringIcons.EventPublisher), "publisher.publishEvent(new OrderEvent(id))")
    }

    fun testListenerInDependencyModuleIsFound() {
        val shared = addDependencyModule("shared")
        addFileToModule(shared, "com/example/events/OrderEvent.java", ORDER_EVENT)
        addFileToModule(shared, "com/example/app/OrderListeners.java", ORDER_LISTENERS)
        val publisher = addFileToModule(module, "com/example/events/OrderPublisher.java", ORDER_PUBLISHER)

        assertSameElements(gutterTargets(publisher, SpringIcons.EventListener), "onOrder(OrderEvent)")
    }

    fun testListenerOfAnotherEventInOtherModuleIsNotFound() {
        addFileToModule(module, "com/example/events/OrderEvent.java", ORDER_EVENT)
        addFileToModule(module, "com/example/events/PaymentEvent.java", PAYMENT_EVENT)
        val publisher = addFileToModule(module, "com/example/events/OrderPublisher.java", ORDER_PUBLISHER)
        val app = addDependentModule("app")
        addFileToModule(app, "com/example/app/OrderListeners.java", ORDER_LISTENERS)
        addFileToModule(app, "com/example/app/PaymentListeners.java", PAYMENT_LISTENERS)

        assertSameElements(gutterTargets(publisher, SpringIcons.EventListener), "onOrder(OrderEvent)")
    }

    fun testApplicationListenerInDependentModuleIsFound() {
        addFileToModule(module, "com/example/events/ShippedEvent.java", SHIPPED_EVENT)
        val publisher = addFileToModule(module, "com/example/events/ShippedPublisher.java", SHIPPED_PUBLISHER)
        val app = addDependentModule("app")
        addFileToModule(app, "com/example/app/ShippedListener.java", SHIPPED_APPLICATION_LISTENER)

        assertSameElements(gutterTargets(publisher, SpringIcons.EventListener), "onApplicationEvent(ShippedEvent)")
    }

    fun testListenerInSiblingModuleOfTheSameApplicationIsFound() {
        val api = addDependencyModule("api")
        addFileToModule(api, "com/example/events/OrderEvent.java", ORDER_EVENT)
        val publisher = addFileToModule(module, "com/example/events/OrderPublisher.java", ORDER_PUBLISHER)
        val listeners = addStandaloneModule("listeners")
        ModuleRootModificationUtil.addDependency(listeners, api)
        val listener = addFileToModule(listeners, "com/example/app/OrderListeners.java", ORDER_LISTENERS)
        val app = addStandaloneModule("app")
        ModuleRootModificationUtil.addDependency(app, module)
        ModuleRootModificationUtil.addDependency(app, listeners)
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        assertFalse(
            "the listener module must be neither a dependency nor a dependent of the publisher module",
            module.moduleWithDependenciesScope.union(module.moduleWithDependentsScope).contains(listener.virtualFile)
        )

        assertSameElements(gutterTargets(publisher, SpringIcons.EventListener), "onOrder(OrderEvent)")
        assertSameElements(gutterTargets(listener, SpringIcons.EventPublisher), "publisher.publishEvent(new OrderEvent(id))")
    }

    fun testListenerInModuleOfAnotherApplicationIsNotFound() {
        val api = addDependencyModule("api")
        addFileToModule(api, "com/example/events/OrderEvent.java", ORDER_EVENT)
        val publisher = addFileToModule(module, "com/example/events/OrderPublisher.java", ORDER_PUBLISHER)
        val other = addStandaloneModule("other")
        ModuleRootModificationUtil.addDependency(other, api)
        addFileToModule(other, "com/example/app/OrderListeners.java", ORDER_LISTENERS)
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        assertEmpty(gutterTargets(publisher, SpringIcons.EventListener))
    }

    fun testPublisherInModuleOfAnotherApplicationIsNotFound() {
        val api = addDependencyModule("api")
        addFileToModule(api, "com/example/events/OrderEvent.java", ORDER_EVENT)
        val listener = addFileToModule(module, "com/example/app/OrderListeners.java", ORDER_LISTENERS)
        val other = addStandaloneModule("other")
        ModuleRootModificationUtil.addDependency(other, api)
        addFileToModule(other, "com/example/events/OrderPublisher.java", ORDER_PUBLISHER)
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        assertEmpty(gutterTargets(listener, SpringIcons.EventPublisher))
    }

    fun testListenersInModuleLibraryAndDependentModuleAreFound() {
        attachSpringBoot(module)
        val publisher = addFileToModule(module, "com/example/events/BootPublisher.java", BOOT_PUBLISHER)
        val app = addDependentModule("app")
        attachSpringBoot(app)
        addFileToModule(app, "com/example/app/AppEnvironmentListener.java", APP_ENVIRONMENT_LISTENER)

        assertEquals(BOOT_ENVIRONMENT_LISTENERS, gutterTargets(publisher, SpringIcons.EventListener).sorted())
    }

    private fun attachSpringBoot(target: Module) {
        ModuleRootModificationUtil.updateModel(target) { model ->
            addFromMaven(model, TestLibrary.springBoot_3_1_1.mavenCoordinates, false)
        }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private fun gutterTargets(file: PsiFile, icon: Icon): List<String> {
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        myFixture.doHighlighting()

        val gutters = myFixture.findAllGutters().filter { it.icon == icon }
        assertEquals("the file must carry exactly one gutter with the expected icon", 1, gutters.size)

        return SpringGutterTestUtil.getGutterTargetsStrings(gutters.single())
    }


    companion object {

        private val BOOT_ENVIRONMENT_LISTENERS = listOf(
            "onApplicationEvent(ApplicationEnvironmentPreparedEvent)",
            "onApplicationEvent(ApplicationEnvironmentPreparedEvent)",
            "onApplicationEvent(ApplicationEnvironmentPreparedEvent)",
            "onApplicationEvent(ApplicationEvent)",
            "onApplicationEvent(SpringApplicationEvent)",
            "onEnvironmentPrepared(ApplicationEnvironmentPreparedEvent)",
        )

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
