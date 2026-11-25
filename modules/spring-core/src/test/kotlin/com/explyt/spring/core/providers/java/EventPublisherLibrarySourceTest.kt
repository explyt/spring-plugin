/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.explyt.spring.test.util.TestCompiledLibrary
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiFile
import com.intellij.psi.search.ProjectScope
import org.intellij.lang.annotations.Language

class EventPublisherLibrarySourceTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testEventListenerParameterFormTargetsLibraryPublishCall() {
        attachVendorLibraryWithSources(VENDOR_EVENT, VENDOR_PUBLISHER)

        val targets = publisherTargets(
            """
            package com.example;

            import com.vendor.events.VendorEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class VendorListener {
                @EventListener
                public void onVendor(VendorEvent event) {}
            }
            """
        )

        assertEquals(listOf(LIBRARY_CALL), targets)
    }

    fun testApplicationListenerTargetsLibraryPublishCall() {
        attachVendorLibraryWithSources(VENDOR_EVENT, VENDOR_PUBLISHER)

        val targets = publisherTargets(
            """
            package com.example;

            import com.vendor.events.VendorEvent;
            import org.springframework.context.ApplicationListener;
            import org.springframework.stereotype.Component;

            @Component
            public class VendorListener implements ApplicationListener<VendorEvent> {
                @Override
                public void onApplicationEvent(VendorEvent event) {}
            }
            """
        )

        assertEquals(listOf(LIBRARY_CALL), targets)
    }

    fun testEventListenerClassesFormTargetsLibraryPublishCall() {
        attachVendorLibraryWithSources(VENDOR_EVENT, VENDOR_PUBLISHER)

        val targets = publisherTargets(
            """
            package com.example;

            import com.vendor.events.VendorEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class VendorListener {
                @EventListener(VendorEvent.class)
                public void onVendor() {}
            }
            """
        )

        assertEquals(listOf(LIBRARY_CALL), targets)
    }

    fun testListenerOfSuperEventTargetsLibraryCallPublishingSubEvent() {
        attachVendorLibraryWithSources(VENDOR_EVENT, VENDOR_SUB_EVENT, VENDOR_SUB_EVENT_PUBLISHER)

        val targets = publisherTargets(
            """
            package com.example;

            import com.vendor.events.VendorEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class VendorListener {
                @EventListener
                public void onVendor(VendorEvent event) {}
            }
            """
        )

        assertEquals(listOf(LIBRARY_SUB_EVENT_CALL), targets)
    }

    fun testListenerOfUnrelatedEventDoesNotTargetLibraryPublishCall() {
        attachVendorLibraryWithSources(VENDOR_EVENT, VENDOR_PUBLISHER)
        myFixture.addClass(
            """
            package com.example;

            import org.springframework.context.ApplicationEvent;

            public class OrderEvent extends ApplicationEvent {
                public OrderEvent(Object source) { super(source); }
            }
            """.trimIndent()
        )

        val targets = publisherTargets(
            """
            package com.example;

            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class OrderListener {
                @EventListener
                public void onOrder(OrderEvent event) {}
            }
            """
        )

        assertEquals(emptyList<String>(), targets)
    }

    fun testProjectAndLibraryPublishCallsAreBothTargets() {
        attachVendorLibraryWithSources(VENDOR_EVENT, VENDOR_PUBLISHER)
        myFixture.addClass(
            """
            package com.example;

            import com.vendor.events.VendorEvent;
            import org.springframework.context.ApplicationEventPublisher;

            public class ProjectPublisher {
                void fire(ApplicationEventPublisher events) {
                    events.publishEvent(new VendorEvent(this));
                }
            }
            """.trimIndent()
        )

        val targets = publisherTargets(
            """
            package com.example;

            import com.vendor.events.VendorEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class VendorListener {
                @EventListener
                public void onVendor(VendorEvent event) {}
            }
            """
        )

        assertSameElements(targets, LIBRARY_CALL, PROJECT_CALL)
    }

    fun testCompiledOnlyLibraryOffersNoPublisherTarget() {
        val library = TestCompiledLibrary.attach(
            myFixture.module, testRootDisposable, LIBRARY_NAME, mapOf(VENDOR_EVENT, VENDOR_PUBLISHER)
        )
        val publisherClass = library.jarRoot.findFileByRelativePath("com/vendor/events/VendorPublisher.class")
            ?: throw AssertionError("the compiled library must contain the publisher class")
        assertTrue(ProjectFileIndex.getInstance(project).isInLibraryClasses(publisherClass))
        assertFalse(ProjectFileIndex.getInstance(project).isInLibrarySource(publisherClass))

        val targets = publisherTargets(
            """
            package com.example;

            import com.vendor.events.VendorEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class VendorListener {
                @EventListener
                public void onVendor(VendorEvent event) {}
            }
            """
        )

        assertEquals(emptyList<String>(), targets)
    }

    private fun attachVendorLibraryWithSources(vararg sources: Pair<String, String>) {
        val publisherSource = TestCompiledLibrary.attach(myFixture.module, testRootDisposable, LIBRARY_NAME, mapOf(*sources))
            .attachSources(myFixture.module, PUBLISHER_PATH)
        assertInLibrarySources(publisherSource)
    }

    private fun assertInLibrarySources(file: PsiFile) {
        val virtualFile = file.virtualFile
        assertTrue("the publisher must be library source", ProjectFileIndex.getInstance(project).isInLibrarySource(virtualFile))
        assertFalse("the publisher must stay outside the project scope", ProjectScope.getProjectScope(project).contains(virtualFile))
    }

    private fun publisherTargets(@Language("JAVA") listener: String): List<String> {
        myFixture.configureByText("VendorListener.java", listener.trimIndent())
        myFixture.doHighlighting()
        val publisherGutter = myFixture.findAllGutters().singleOrNull { it.icon == SpringIcons.EventPublisher }
            ?: throw AssertionError("the listener method must carry exactly one event publisher gutter")
        return SpringGutterTestUtil.getGutterTargetsStrings(publisherGutter)
    }

    private companion object {
        const val LIBRARY_NAME = "vendor-events"
        const val PUBLISHER_PATH = "com/vendor/events/VendorPublisher.java"
        const val LIBRARY_CALL = "publisher.publishEvent(new VendorEvent(this))"
        const val LIBRARY_SUB_EVENT_CALL = "publisher.publishEvent(new VendorSubEvent(this))"
        const val PROJECT_CALL = "events.publishEvent(new VendorEvent(this))"

        val VENDOR_EVENT = "com/vendor/events/VendorEvent.java" to """
            package com.vendor.events;

            import org.springframework.context.ApplicationEvent;

            public class VendorEvent extends ApplicationEvent {
                public VendorEvent(Object source) { super(source); }
            }
        """.trimIndent()

        val VENDOR_SUB_EVENT = "com/vendor/events/VendorSubEvent.java" to """
            package com.vendor.events;

            public class VendorSubEvent extends VendorEvent {
                public VendorSubEvent(Object source) { super(source); }
            }
        """.trimIndent()

        val VENDOR_PUBLISHER = PUBLISHER_PATH to """
            package com.vendor.events;

            import org.springframework.context.ApplicationEventPublisher;

            public class VendorPublisher {
                public void ready(ApplicationEventPublisher publisher) {
                    publisher.publishEvent(new VendorEvent(this));
                }
            }
        """.trimIndent()

        val VENDOR_SUB_EVENT_PUBLISHER = PUBLISHER_PATH to """
            package com.vendor.events;

            import org.springframework.context.ApplicationEventPublisher;

            public class VendorPublisher {
                public void ready(ApplicationEventPublisher publisher) {
                    publisher.publishEvent(new VendorSubEvent(this));
                }
            }
        """.trimIndent()
    }
}
