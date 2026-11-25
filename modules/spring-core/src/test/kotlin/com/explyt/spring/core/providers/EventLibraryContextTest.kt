/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.explyt.spring.test.util.TestCompiledLibrary
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.testFramework.IndexingTestUtil
import javax.swing.Icon

class EventLibraryContextTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testListenerInLibraryOfDependentModuleIsFound() {
        val events = attachEvents(module)
        val publisher = addFileToModule(module, "com/example/OrderPublisher.java", PROJECT_PUBLISHER)
        val app = addDependentModule("app")
        events.attachTo(app, testRootDisposable)
        attachLibrary(app, "vendor-listeners", LISTENERS)

        assertEquals(listOf("onVendor(VendorEvent)"), targets(publisher, SpringIcons.EventListener))
    }

    fun testListenerInLibraryOfUnrelatedModuleIsNotFound() {
        val events = attachEvents(module)
        val publisher = addFileToModule(module, "com/example/OrderPublisher.java", PROJECT_PUBLISHER)
        val other = addStandaloneModule("other")
        events.attachTo(other, testRootDisposable)
        attachLibrary(other, "vendor-listeners", LISTENERS)

        assertEmpty(targets(publisher, SpringIcons.EventListener))
    }

    fun testCompiledListenerDeclaringEventClassesIsFound() {
        attachEvents(module)
        attachLibrary(module, "vendor-listeners", CLASSES_LISTENERS)
        val publisher = addFileToModule(module, "com/example/OrderPublisher.java", PROJECT_PUBLISHER)

        assertEquals(listOf("onAnyVendor()"), targets(publisher, SpringIcons.EventListener))
    }

    fun testPublishCallInLibrarySourcesOfDependentModuleIsFound() {
        val events = attachEvents(module)
        val listener = addFileToModule(module, "com/example/OrderListener.java", PROJECT_LISTENER)
        val app = addDependentModule("app")
        events.attachTo(app, testRootDisposable)
        assertInLibrarySources(attachLibraryWithSources(app, "vendor-publishers", PUBLISHERS, PUBLISHER_PATH))

        assertEquals(listOf(LIBRARY_CALL), targets(listener, SpringIcons.EventPublisher))
    }

    fun testPublishCallInLibrarySourcesOfUnrelatedModuleIsNotFound() {
        val events = attachEvents(module)
        val listener = addFileToModule(module, "com/example/OrderListener.java", PROJECT_LISTENER)
        val other = addStandaloneModule("other")
        events.attachTo(other, testRootDisposable)
        assertInLibrarySources(attachLibraryWithSources(other, "vendor-publishers", PUBLISHERS, PUBLISHER_PATH))

        assertEmpty(targets(listener, SpringIcons.EventPublisher))
    }

    fun testListenerInLibrarySourcesFindsPublisherOfAnyModule() {
        attachEvents(module)
        val listenerSource = attachLibraryWithSources(module, "vendor-listeners", LISTENERS, LISTENER_PATH)
        assertInLibrarySources(listenerSource)
        val other = addStandaloneModule("other")
        TestCompiledLibrary.attach(other, testRootDisposable, "vendor-events-other", mapOf(EVENT))
        addFileToModule(other, "com/example/OrderPublisher.java", PROJECT_PUBLISHER)
        assertNull(
            "the listener lives in library sources and must belong to no module",
            ModuleUtilCore.findModuleForFile(listenerSource.virtualFile, project)
        )

        assertEquals(listOf(PROJECT_CALL), targets(listenerSource, SpringIcons.EventPublisher))
    }

    private fun attachEvents(target: Module): TestCompiledLibrary =
        TestCompiledLibrary.attach(target, testRootDisposable, "vendor-events", mapOf(EVENT))

    private fun attachLibrary(target: Module, name: String, source: Pair<String, String>): TestCompiledLibrary {
        val library = TestCompiledLibrary.attach(target, testRootDisposable, name, mapOf(source))
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        return library
    }

    private fun attachLibraryWithSources(
        target: Module, name: String, source: Pair<String, String>, path: String
    ): PsiFile = attachLibrary(target, name, source).attachSources(target, path)

    private fun assertInLibrarySources(file: PsiFile) {
        val virtualFile: VirtualFile = file.virtualFile
        assertTrue(
            "the file must be library source",
            ProjectFileIndex.getInstance(project).isInLibrarySource(virtualFile)
        )
    }

    private fun targets(file: PsiFile, icon: Icon): List<String> {
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        myFixture.doHighlighting()
        val gutters = myFixture.findAllGutters().filter { it.icon == icon }
        assertEquals("the file must carry exactly one gutter with the expected icon", 1, gutters.size)
        return SpringGutterTestUtil.getGutterTargetsStrings(gutters.single())
    }

    private companion object {
        const val LISTENER_PATH = "com/vendor/listeners/VendorListeners.java"
        const val PUBLISHER_PATH = "com/vendor/publishers/VendorPublisher.java"
        const val LIBRARY_CALL = "publisher.publishEvent(new VendorEvent())"
        const val PROJECT_CALL = "events.publishEvent(new VendorEvent())"

        val EVENT = "com/vendor/events/VendorEvent.java" to """
            package com.vendor.events;

            public class VendorEvent {
            }
        """.trimIndent()

        val LISTENERS = LISTENER_PATH to """
            package com.vendor.listeners;

            import com.vendor.events.VendorEvent;
            import org.springframework.context.event.EventListener;

            public class VendorListeners {
                @EventListener
                public void onVendor(VendorEvent event) {}
            }
        """.trimIndent()

        val CLASSES_LISTENERS = LISTENER_PATH to """
            package com.vendor.listeners;

            import com.vendor.events.VendorEvent;
            import org.springframework.context.event.EventListener;

            public class VendorListeners {
                @EventListener(VendorEvent.class)
                public void onAnyVendor() {}
            }
        """.trimIndent()

        val PUBLISHERS = PUBLISHER_PATH to """
            package com.vendor.publishers;

            import com.vendor.events.VendorEvent;
            import org.springframework.context.ApplicationEventPublisher;

            public class VendorPublisher {
                public void ready(ApplicationEventPublisher publisher) {
                    publisher.publishEvent(new VendorEvent());
                }
            }
        """.trimIndent()

        const val PROJECT_PUBLISHER = """
            package com.example;

            import com.vendor.events.VendorEvent;
            import org.springframework.context.ApplicationEventPublisher;

            public class OrderPublisher {
                public void fire(ApplicationEventPublisher events) {
                    events.publishEvent(new VendorEvent());
                }
            }
        """

        const val PROJECT_LISTENER = """
            package com.example;

            import com.vendor.events.VendorEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class OrderListener {
                @EventListener
                public void onVendor(VendorEvent event) {}
            }
        """
    }
}
