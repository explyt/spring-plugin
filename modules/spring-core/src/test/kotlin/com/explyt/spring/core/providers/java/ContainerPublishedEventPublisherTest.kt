/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.java

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.util.ExplytPsiUtil.isEqualOrInheritor
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.intellij.codeInsight.daemon.GutterMark
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.intellij.lang.annotations.Language

/**
 * A listener of a container-published event has no `publishEvent` call to navigate to: Spring fires those
 * events from inside a jar, whose sources root is outside every module search scope. The event declaration
 * is offered instead of an empty popup claiming no publisher exists.
 */
class ContainerPublishedEventPublisherTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_5_0, TestLibrary.springSecurityCore_6_0_7
    )

    fun testBootLifecycleEventResolvesToItsDeclaration() {
        @Language("JAVA") val source = """
            package com.example;

            import org.springframework.boot.context.event.ApplicationReadyEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class BootGuard {
                @EventListener(ApplicationReadyEvent.class)
                public void validateOnBoot() {
                }
            }
        """.trimIndent()

        assertTargetsOfPublisherGutter(source, "ApplicationReadyEvent")
    }

    fun testParameterFormResolvesToItsDeclaration() {
        @Language("JAVA") val source = """
            package com.example;

            import org.springframework.boot.context.event.ApplicationReadyEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class BootGuard {
                @EventListener
                public void onReady(ApplicationReadyEvent event) {
                }
            }
        """.trimIndent()

        assertTargetsOfPublisherGutter(source, "ApplicationReadyEvent")
    }

    fun testContextEventResolvesToItsDeclaration() {
        @Language("JAVA") val source = """
            package com.example;

            import org.springframework.context.event.ContextClosedEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class ShutdownProbe {
                @EventListener
                public void onClose(ContextClosedEvent event) {
                }
            }
        """.trimIndent()

        assertTargetsOfPublisherGutter(source, "ContextClosedEvent")
    }

    /**
     * The defect this guards against: an application event that merely *extends* a lifecycle base is not
     * container-published, and must keep resolving to the application's own call. Detection by inheritance
     * alone reports the event declaration here and loses that call.
     */
    fun testApplicationSubclassOfLifecycleEventKeepsItsOwnPublisher() {
        @Language("JAVA") val source = """
            package com.example;

            import org.springframework.context.ApplicationContext;
            import org.springframework.context.ApplicationEventPublisher;
            import org.springframework.context.event.ContextStartedEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            public class Blocklist {

                public static class BlockedListStartedEvent extends ContextStartedEvent {
                    public BlockedListStartedEvent(ApplicationContext source) {
                        super(source);
                    }
                }

                @Component
                public static class Notifier {
                    @EventListener
                    public void onStarted(BlockedListStartedEvent event) {
                    }
                }

                @Component
                public static class EmailService {
                    private final ApplicationEventPublisher publisher;

                    public EmailService(ApplicationEventPublisher publisher) {
                        this.publisher = publisher;
                    }

                    public void startedEvent(ApplicationContext context) {
                        publisher.publishEvent(new BlockedListStartedEvent(context));
                    }
                }
            }
        """.trimIndent()

        myFixture.configureByText("Blocklist.java", source)
        myFixture.doHighlighting()

        assertEventIsDeclaredInProjectSources("com.example.Blocklist.BlockedListStartedEvent")
        assertLifecycleBaseIsResolvable(APPLICATION_CONTEXT_EVENT)

        val targets = SpringGutterTestUtil.getGutterTargetsStrings(publisherGutter())

        assertEquals(
            "an application event must resolve to its own publish call only, but was: $targets",
            listOf("publisher.publishEvent(new BlockedListStartedEvent(context))"), targets
        )
    }

    /**
     * Spring Security publishes its own events from library code exactly as the container does, but
     * `AbstractAuthenticationEvent` extends `ApplicationEvent` directly, so neither lifecycle base matches it.
     */
    fun testSecurityEventResolvesToItsDeclaration() {
        @Language("JAVA") val source = """
            package com.example;

            import org.springframework.context.ApplicationListener;
            import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
            import org.springframework.stereotype.Component;

            @Component
            public class LoginAudit implements ApplicationListener<AuthenticationSuccessEvent> {
                @Override
                public void onApplicationEvent(AuthenticationSuccessEvent event) {
                }
            }
        """.trimIndent()

        assertSecurityEventIsOutsideTheLifecycleBases()
        assertTargetsOfPublisherGutter(source, "AuthenticationSuccessEvent")
    }

    fun testSecurityEventAnnotationFormResolvesToItsDeclaration() {
        @Language("JAVA") val source = """
            package com.example;

            import org.springframework.context.event.EventListener;
            import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
            import org.springframework.stereotype.Component;

            @Component
            public class LoginAudit {
                @EventListener(AuthenticationSuccessEvent.class)
                public void onLogin() {
                }
            }
        """.trimIndent()

        assertSecurityEventIsOutsideTheLifecycleBases()
        assertTargetsOfPublisherGutter(source, "AuthenticationSuccessEvent")
    }

    /**
     * A library event the application publishes itself has a real call to navigate to, so no declaration may be
     * offered for it. This is the case that stops "declared in a library" alone from being the rule.
     */
    fun testLibraryEventPublishedByApplicationKeepsItsOwnPublisher() {
        @Language("JAVA") val source = """
            package com.example;

            import org.springframework.context.ApplicationContext;
            import org.springframework.context.ApplicationEventPublisher;
            import org.springframework.context.event.ContextStartedEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            public class SelfPublished {

                @Component
                public static class Notifier {
                    @EventListener
                    public void onStarted(ContextStartedEvent event) {
                    }
                }

                @Component
                public static class Trigger {
                    private final ApplicationEventPublisher publisher;

                    public Trigger(ApplicationEventPublisher publisher) {
                        this.publisher = publisher;
                    }

                    public void fire(ApplicationContext context) {
                        publisher.publishEvent(new ContextStartedEvent(context));
                    }
                }
            }
        """.trimIndent()

        myFixture.configureByText("SelfPublished.java", source)
        myFixture.doHighlighting()

        assertEventIsDeclaredInLibrary("org.springframework.context.event.ContextStartedEvent")

        val targets = SpringGutterTestUtil.getGutterTargetsStrings(publisherGutter())

        assertEquals(
            "a library event published by the application must resolve to that call only, but was: $targets",
            listOf("publisher.publishEvent(new ContextStartedEvent(context))"), targets
        )
    }

    /**
     * Lazy targets keep the icon installed with nothing to navigate to, so the balloon is the entire answer a
     * user gets. It must state what the search found, not that no publisher exists anywhere.
     */
    fun testUnpublishedApplicationEventReportsWhatTheSearchCovered() {
        @Language("JAVA") val source = """
            package com.example;

            import org.springframework.context.ApplicationEvent;
            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            public class Unpublished {

                public static class NeverPublishedEvent extends ApplicationEvent {
                    public NeverPublishedEvent(Object source) {
                        super(source);
                    }
                }

                @Component
                public static class Listener {
                    @EventListener
                    public void onNeverPublished(NeverPublishedEvent event) {
                    }
                }
            }
        """.trimIndent()

        myFixture.configureByText("Unpublished.java", source)
        myFixture.doHighlighting()

        val gutter = publisherGutter()
        assertEmpty(
            "the empty state proves nothing unless the event really has no publisher",
            SpringGutterTestUtil.getGutterTargetsStrings(gutter)
        )

        assertEquals(
            "No publisher found in project sources",
            SpringGutterTestUtil.getGutterEmptyText(gutter)
        )
    }

    private fun assertTargetsOfPublisherGutter(source: String, expectedEventName: String) {
        myFixture.configureByText("Listener.java", source)
        myFixture.doHighlighting()

        assertLifecycleBaseIsResolvable(SpringCoreClasses.APPLICATION_EVENT)

        val targets = SpringGutterTestUtil.getGutterTargetsStrings(publisherGutter())

        assertEquals("the event declaration must be the only target, but was: $targets", 1, targets.size)
        assertEquals(expectedEventName, targets.single())
    }

    /**
     * Without this the security tests would prove nothing new: they must fail under the two-lifecycle-base
     * rule and pass only once detection is keyed on `ApplicationEvent` itself.
     */
    private fun assertSecurityEventIsOutsideTheLifecycleBases() {
        val securityEvent = LibraryClassCache.searchForLibraryClass(myFixture.module, SECURITY_SUCCESS_EVENT)
            ?: throw AssertionError("$SECURITY_SUCCESS_EVENT must resolve in the fixture")
        for (lifecycleBase in listOf(SPRING_APPLICATION_EVENT, APPLICATION_CONTEXT_EVENT)) {
            val baseClass = LibraryClassCache.searchForLibraryClass(myFixture.module, lifecycleBase) ?: continue
            assertFalse(
                "$SECURITY_SUCCESS_EVENT must not inherit $lifecycleBase, otherwise this case is already covered",
                securityEvent.isEqualOrInheritor(baseClass)
            )
        }
    }

    private fun assertEventIsDeclaredInLibrary(eventFqn: String) {
        val eventClass = JavaPsiFacade.getInstance(project).findClass(eventFqn, GlobalSearchScope.allScope(project))
            ?: throw AssertionError("the fixture must resolve $eventFqn")
        val virtualFile = eventClass.containingFile.virtualFile
            ?: throw AssertionError("$eventFqn must be backed by a VirtualFile")
        assertTrue(
            "the contrast proves nothing unless the event is declared in a library",
            ProjectFileIndex.getInstance(project).isInLibrary(virtualFile)
        )
    }

    /**
     * Restating the hierarchy as a literal would pass even if Spring moved the base class, so the constant is
     * checked against the library that ships it.
     */
    private fun assertLifecycleBaseIsResolvable(baseClassFqn: String) {
        assertNotNull(
            "$baseClassFqn must resolve in the fixture, otherwise detection cannot classify anything",
            LibraryClassCache.searchForLibraryClass(myFixture.module, baseClassFqn)
        )
    }

    private fun assertEventIsDeclaredInProjectSources(eventFqn: String) {
        val eventClass = JavaPsiFacade.getInstance(project).findClass(eventFqn, GlobalSearchScope.allScope(project))
            ?: throw AssertionError("the fixture must declare $eventFqn")
        val virtualFile = eventClass.containingFile.virtualFile
            ?: throw AssertionError("$eventFqn must be backed by a VirtualFile")
        assertFalse(
            "the contrast proves nothing unless the event is declared in project sources",
            ProjectFileIndex.getInstance(project).isInLibrary(virtualFile)
        )
    }

    private fun publisherGutter(): GutterMark {
        val gutters = myFixture.findAllGutters().filter { it.icon == SpringIcons.EventPublisher }
        assertEquals("exactly one event publisher gutter is expected", 1, gutters.size)
        return gutters.single()
    }

    private companion object {
        const val SECURITY_SUCCESS_EVENT =
            "org.springframework.security.authentication.event.AuthenticationSuccessEvent"
        const val SPRING_APPLICATION_EVENT = "org.springframework.boot.context.event.SpringApplicationEvent"
        const val APPLICATION_CONTEXT_EVENT = "org.springframework.context.event.ApplicationContextEvent"
    }
}
