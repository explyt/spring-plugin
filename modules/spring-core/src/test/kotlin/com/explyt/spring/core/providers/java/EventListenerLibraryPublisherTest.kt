/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.explyt.spring.test.util.TestLibrarySourceRoot
import com.intellij.codeInsight.daemon.GutterMark
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiFile
import org.intellij.lang.annotations.Language

/**
 * Most lifecycle events are published from inside a library, not from application code: Spring Boot's own
 * `EventPublishingRunListener.ready` is what fires `ApplicationReadyEvent`. The gutter on such a call is
 * installed without needing a module, so it must also be able to find its listeners without one.
 */
class EventListenerLibraryPublisherTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testListenerIsFoundFromLibraryPublishEventCall() {
        addListenerToProject()
        val libraryFile = addPublisherToLibrary()

        assertPublishEventCallIsInsideLibrary(libraryFile)

        val targets = eventListenerGutterTargets(libraryFile)

        assertEquals("the single listener of the published event must be the only target", 1, targets.size)
        assertEquals("onReady(CustomEvent)", targets.single())
    }

    /**
     * `ApplicationListener.onApplicationEvent` is discovered by a different branch
     * (`ClassInheritorsSearch`) than `@EventListener` (`AnnotatedElementsSearch`), and both branches take the
     * same module argument, so both have to survive a library-originated call.
     */
    fun testApplicationListenerIsFoundFromLibraryPublishEventCall() {
        myFixture.addClass(
            """
            package com.example;

            import org.springframework.context.ApplicationListener;
            import org.springframework.stereotype.Component;

            @Component
            public class BootListener implements ApplicationListener<CustomEvent> {
                @Override
                public void onApplicationEvent(CustomEvent event) {}
            }
            """.trimIndent()
        )
        addEventToProject()
        val libraryFile = addPublisherToLibrary()

        assertPublishEventCallIsInsideLibrary(libraryFile)

        val targets = eventListenerGutterTargets(libraryFile)

        assertEquals("the ApplicationListener implementation must be the only target", 1, targets.size)
        assertEquals("onApplicationEvent(CustomEvent)", targets.single())
    }

    private fun addEventToProject() {
        myFixture.addClass(
            """
            package com.example;

            import org.springframework.context.ApplicationEvent;

            public class CustomEvent extends ApplicationEvent {
                public CustomEvent(Object source) { super(source); }
            }
            """.trimIndent()
        )
    }

    private fun addListenerToProject() {
        addEventToProject()
        myFixture.addClass(
            """
            package com.example;

            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class BootGuard {
                @EventListener
                public void onReady(CustomEvent event) {}
            }
            """.trimIndent()
        )
    }

    private fun addPublisherToLibrary(): PsiFile {
        @Language("JAVA") val publisher = """
            package org.springframework.boot.context.event;

            import com.example.CustomEvent;
            import org.springframework.context.ConfigurableApplicationContext;

            public class EventPublishingRunListener {
                public void ready(ConfigurableApplicationContext context) {
                    context.publishEvent(new CustomEvent(this));
                }
            }
        """.trimIndent()

        return TestLibrarySourceRoot.create(myFixture.module, testRootDisposable, "spring-boot-sources")
            .addFile("org/springframework/boot/context/event/EventPublishingRunListener.java", publisher)
    }

    /**
     * Without this the test is vacuous: a publisher written into a module source root is ordinary project
     * code, which already resolves, so the assertions below would pass with the defect present.
     */
    private fun assertPublishEventCallIsInsideLibrary(libraryFile: PsiFile) {
        val virtualFile = libraryFile.virtualFile
            ?: throw AssertionError("the library source file must be backed by a VirtualFile")
        assertTrue(
            "the fixture only reproduces the defect when the publisher lives in library sources",
            ProjectFileIndex.getInstance(project).isInLibrarySource(virtualFile)
        )
        assertNull(
            "the platform must not resolve a module for library PSI, otherwise this fixture proves nothing",
            ModuleUtilCore.findModuleForPsiElement(publishEventCall(libraryFile))
        )
    }

    private fun publishEventCall(libraryFile: PsiFile) =
        libraryFile.findElementAt(libraryFile.text.indexOf("publishEvent"))
            ?: throw AssertionError("the fixture must contain a publishEvent call")

    private fun eventListenerGutterTargets(libraryFile: PsiFile): List<String> {
        myFixture.configureFromExistingVirtualFile(libraryFile.virtualFile)
        myFixture.doHighlighting()

        val listenerGutters: List<GutterMark> = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.EventListener }
        assertEquals("the publishEvent call must carry exactly one event listener gutter", 1, listenerGutters.size)

        return SpringGutterTestUtil.getGutterTargetsStrings(listenerGutters.single())
    }
}
