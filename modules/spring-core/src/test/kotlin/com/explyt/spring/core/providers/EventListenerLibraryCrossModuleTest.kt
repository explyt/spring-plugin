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
import com.explyt.spring.test.util.TestLibrarySourceRoot
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil
import org.intellij.lang.annotations.Language

/**
 * A `publishEvent` call inside a library has no owning module: the platform answers with whichever attached
 * module sorts first by dependency order. Searching only that module's dependency closure hides listeners
 * declared in sibling modules, so the gutter's answer would depend on which module happened to win.
 *
 * The event class lives in the library too, mirroring `ApplicationReadyEvent` — an event declared in an
 * application module could not be resolved from the library's own source at all.
 */
class EventListenerLibraryCrossModuleTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testListenerInSiblingModuleIsFoundFromLibraryPublishEventCall() {
        val sibling = addUnrelatedModule()

        val library = TestLibrarySourceRoot.create(module, testRootDisposable, LIBRARY_NAME)
        library.addFile("org/springframework/boot/context/event/LibraryReadyEvent.java", LIBRARY_EVENT)
        val libraryFile = library
            .addFile("org/springframework/boot/context/event/EventPublishingRunListener.java", LIBRARY_PUBLISHER)
        library.attachTo(sibling)

        // Which attached module the platform anchors the call in is its own choice, so the listener goes into
        // whichever module it did *not* pick — that is the only placement the defect can be observed from.
        val publisherModule = ModuleUtilCore.findModuleForPsiElement(libraryFile)
            ?: throw AssertionError("the library must be attached to some module")
        val listenerModule = if (publisherModule == sibling) module else sibling
        addFileToModule(listenerModule, "com/example/listeners/SiblingGuard.java", SIBLING_LISTENER)

        assertSiblingListenerIsOutsideThePublisherModuleScope(publisherModule, listenerModule)

        val targets = eventListenerGutterTargets(libraryFile)

        assertEquals(
            "the listener in module '${listenerModule.name}' must be found from the call the platform " +
                    "anchored in module '${publisherModule.name}'",
            1, targets.size
        )
        assertEquals("onSibling(LibraryReadyEvent)", targets.single())
    }

    /**
     * The fixture proves nothing unless the listener really sits outside the module the platform picks for the
     * library call: with a dependency edge in place the old single-module search would have found it anyway.
     */
    private fun assertSiblingListenerIsOutsideThePublisherModuleScope(
        publisherModule: Module,
        listenerModule: Module
    ) {
        val listenerFile = ModuleRootManager.getInstance(listenerModule).sourceRoots.first()
            .findFileByRelativePath("com/example/listeners/SiblingGuard.java")
            ?: throw AssertionError("the sibling listener file must exist")

        assertFalse(
            "the sibling listener must be invisible to the publisher module's dependency scope, " +
                    "otherwise a single-module search would already find it",
            GlobalSearchScope.moduleWithDependenciesScope(publisherModule).contains(listenerFile)
        )
        assertTrue(
            "the widened scope must contain the sibling listener, otherwise the fix cannot find it",
            GlobalSearchScope.projectScope(project).contains(listenerFile)
        )
    }

    private fun addUnrelatedModule(): Module {
        val name = "listeners"
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("$name/src")
        val unrelated = PsiTestUtil.addModule(project, JavaModuleType.getModuleType(), name, sourceRoot)
        ModuleRootModificationUtil.setSdkInherited(unrelated)
        ModuleRootModificationUtil.updateModel(unrelated) { model ->
            libraries.forEach { addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies) }
        }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        return unrelated
    }

    private fun eventListenerGutterTargets(libraryFile: PsiFile): List<String> {
        myFixture.configureFromExistingVirtualFile(libraryFile.virtualFile)
        myFixture.doHighlighting()

        val listenerGutters = myFixture.findAllGutters().filter { it.icon == SpringIcons.EventListener }
        assertEquals("the publishEvent call must carry exactly one event listener gutter", 1, listenerGutters.size)

        return SpringGutterTestUtil.getGutterTargetsStrings(listenerGutters.single())
    }

    companion object {
        private const val LIBRARY_NAME = "spring-boot-cross-module-sources"

        @Language("JAVA")
        private val LIBRARY_EVENT = """
            package org.springframework.boot.context.event;

            import org.springframework.context.ApplicationEvent;

            public class LibraryReadyEvent extends ApplicationEvent {
                public LibraryReadyEvent(Object source) { super(source); }
            }
        """.trimIndent()

        @Language("JAVA")
        private val LIBRARY_PUBLISHER = """
            package org.springframework.boot.context.event;

            import org.springframework.context.ConfigurableApplicationContext;

            public class EventPublishingRunListener {
                public void ready(ConfigurableApplicationContext context) {
                    context.publishEvent(new LibraryReadyEvent(this));
                }
            }
        """.trimIndent()

        @Language("JAVA")
        private val SIBLING_LISTENER = """
            package com.example.listeners;

            import org.springframework.boot.context.event.LibraryReadyEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class SiblingGuard {
                @EventListener
                public void onSibling(LibraryReadyEvent event) {}
            }
        """.trimIndent()
    }
}
