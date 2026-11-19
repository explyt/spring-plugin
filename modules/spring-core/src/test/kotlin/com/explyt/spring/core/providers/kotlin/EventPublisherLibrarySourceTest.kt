/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.explyt.spring.test.util.TestCompiledLibrary
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.search.ProjectScope
import org.intellij.lang.annotations.Language

class EventPublisherLibrarySourceTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testEventListenerParameterFormTargetsLibraryPublishCall() {
        attachVendorLibraryWithSources()

        val targets = publisherTargets(
            """
            package com.example

            import com.vendor.events.VendorEvent
            import org.springframework.context.event.EventListener
            import org.springframework.stereotype.Component

            @Component
            class VendorListener {
                @EventListener
                fun onVendor(event: VendorEvent) {}
            }
            """
        )

        assertEquals(listOf(LIBRARY_CALL), targets)
    }

    fun testApplicationListenerTargetsLibraryPublishCall() {
        attachVendorLibraryWithSources()

        val targets = publisherTargets(
            """
            package com.example

            import com.vendor.events.VendorEvent
            import org.springframework.context.ApplicationListener
            import org.springframework.stereotype.Component

            @Component
            class VendorListener : ApplicationListener<VendorEvent> {
                override fun onApplicationEvent(event: VendorEvent) {}
            }
            """
        )

        assertEquals(listOf(LIBRARY_CALL), targets)
    }

    fun testEventListenerClassesFormTargetsLibraryPublishCall() {
        attachVendorLibraryWithSources()

        val targets = publisherTargets(
            """
            package com.example

            import com.vendor.events.VendorEvent
            import org.springframework.context.event.EventListener
            import org.springframework.stereotype.Component

            @Component
            class VendorListener {
                @EventListener(VendorEvent::class)
                fun onVendor() {}
            }
            """
        )

        assertEquals(listOf(LIBRARY_CALL), targets)
    }

    private fun attachVendorLibraryWithSources() {
        val publisherSource = TestCompiledLibrary.attach(
            myFixture.module, testRootDisposable, "vendor-events", mapOf(VENDOR_EVENT, VENDOR_PUBLISHER)
        ).attachSources(myFixture.module, PUBLISHER_PATH)
        val virtualFile = publisherSource.virtualFile
        assertTrue("the publisher must be library source", ProjectFileIndex.getInstance(project).isInLibrarySource(virtualFile))
        assertFalse("the publisher must stay outside the project scope", ProjectScope.getProjectScope(project).contains(virtualFile))
    }

    private fun publisherTargets(@Language("kotlin") listener: String): List<String> {
        myFixture.configureByText("VendorListener.kt", listener.trimIndent())
        myFixture.doHighlighting()
        val publisherGutter = myFixture.findAllGutters().singleOrNull { it.icon == SpringIcons.EventPublisher }
            ?: throw AssertionError("the listener function must carry exactly one event publisher gutter")
        return SpringGutterTestUtil.getGutterTargetsStrings(publisherGutter)
    }

    private companion object {
        const val PUBLISHER_PATH = "com/vendor/events/VendorPublisher.java"
        const val LIBRARY_CALL = "publisher.publishEvent(new VendorEvent(this))"

        val VENDOR_EVENT = "com/vendor/events/VendorEvent.java" to """
            package com.vendor.events;

            import org.springframework.context.ApplicationEvent;

            public class VendorEvent extends ApplicationEvent {
                public VendorEvent(Object source) { super(source); }
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
    }
}
