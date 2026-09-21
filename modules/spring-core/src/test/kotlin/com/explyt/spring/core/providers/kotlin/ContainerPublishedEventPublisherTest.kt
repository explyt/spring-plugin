/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.kotlin

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.util.ExplytPsiUtil.isEqualOrInheritor
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.intellij.codeInsight.daemon.GutterMark
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.intellij.lang.annotations.Language

/**
 * Kotlin twin: `@EventListener(X::class)` reaches the provider as a `UClassLiteralExpression` built from a
 * Kotlin class literal, a different resolution path than Java's `X.class`.
 */
class ContainerPublishedEventPublisherTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_5_0, TestLibrary.springSecurityCore_6_0_7
    )

    fun testBootLifecycleEventResolvesToItsDeclaration() {
        @Language("kotlin") val source = """
            package com.example

            import org.springframework.boot.context.event.ApplicationReadyEvent
            import org.springframework.context.event.EventListener
            import org.springframework.stereotype.Component

            @Component
            class BootGuard {
                @EventListener(ApplicationReadyEvent::class)
                fun validateOnBoot() {
                }
            }
        """.trimIndent()

        assertTargetsOfPublisherGutter(source, "ApplicationReadyEvent")
    }

    fun testParameterFormResolvesToItsDeclaration() {
        @Language("kotlin") val source = """
            package com.example

            import org.springframework.boot.context.event.ApplicationReadyEvent
            import org.springframework.context.event.EventListener
            import org.springframework.stereotype.Component

            @Component
            class BootGuard {
                @EventListener
                fun onReady(event: ApplicationReadyEvent) {
                }
            }
        """.trimIndent()

        assertTargetsOfPublisherGutter(source, "ApplicationReadyEvent")
    }

    /**
     * Detection by inheritance alone would replace this application event's own publish call with the event
     * declaration; the call must stay.
     */
    fun testApplicationSubclassOfLifecycleEventKeepsItsOwnPublisher() {
        @Language("kotlin") val source = """
            package com.example

            import org.springframework.context.ApplicationContext
            import org.springframework.context.ApplicationEventPublisher
            import org.springframework.context.event.ContextStartedEvent
            import org.springframework.context.event.EventListener
            import org.springframework.stereotype.Component

            class BlockedListStartedEvent(source: ApplicationContext) : ContextStartedEvent(source)

            @Component
            class Notifier {
                @EventListener
                fun onStarted(event: BlockedListStartedEvent) {
                }
            }

            @Component
            class EmailService(private val publisher: ApplicationEventPublisher) {
                fun startedEvent(context: ApplicationContext) {
                    publisher.publishEvent(BlockedListStartedEvent(context))
                }
            }
        """.trimIndent()

        myFixture.configureByText("Blocklist.kt", source)
        myFixture.doHighlighting()

        assertEventIsDeclaredInProjectSources("com.example.BlockedListStartedEvent")
        assertLifecycleBaseIsResolvable(APPLICATION_CONTEXT_EVENT)

        val targets = SpringGutterTestUtil.getGutterTargetsStrings(publisherGutter())

        assertEquals(
            "an application event must resolve to its own publish call only, but was: $targets",
            listOf("publishEvent(BlockedListStartedEvent(context))"), targets
        )
    }

    /**
     * Spring Security publishes its own events from library code exactly as the container does, but
     * `AbstractAuthenticationEvent` extends `ApplicationEvent` directly, so neither lifecycle base matches it.
     */
    fun testSecurityEventResolvesToItsDeclaration() {
        @Language("kotlin") val source = """
            package com.example

            import org.springframework.context.event.EventListener
            import org.springframework.security.authentication.event.AuthenticationSuccessEvent
            import org.springframework.stereotype.Component

            @Component
            class LoginAudit {
                @EventListener(AuthenticationSuccessEvent::class)
                fun onLogin() {
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
        @Language("kotlin") val source = """
            package com.example

            import org.springframework.context.ApplicationContext
            import org.springframework.context.ApplicationEventPublisher
            import org.springframework.context.event.ContextStartedEvent
            import org.springframework.context.event.EventListener
            import org.springframework.stereotype.Component

            @Component
            class Notifier {
                @EventListener
                fun onStarted(event: ContextStartedEvent) {
                }
            }

            @Component
            class Trigger(private val publisher: ApplicationEventPublisher) {
                fun fire(context: ApplicationContext) {
                    publisher.publishEvent(ContextStartedEvent(context))
                }
            }
        """.trimIndent()

        myFixture.configureByText("SelfPublished.kt", source)
        myFixture.doHighlighting()

        assertEventIsDeclaredInLibrary("org.springframework.context.event.ContextStartedEvent")

        val targets = SpringGutterTestUtil.getGutterTargetsStrings(publisherGutter())

        assertEquals(
            "a library event published by the application must resolve to that call only, but was: $targets",
            listOf("publishEvent(ContextStartedEvent(context))"), targets
        )
    }

    private fun assertTargetsOfPublisherGutter(source: String, expectedEventName: String) {
        myFixture.configureByText("Listener.kt", source)
        myFixture.doHighlighting()

        assertLifecycleBaseIsResolvable(SpringCoreClasses.APPLICATION_EVENT)

        val targets = SpringGutterTestUtil.getGutterTargetsStrings(publisherGutter())

        assertEquals("the event declaration must be the only target, but was: $targets", 1, targets.size)
        assertEquals(expectedEventName, targets.single())
    }

    /**
     * Without this the security test would prove nothing new: it must fail under the two-lifecycle-base rule
     * and pass only once detection is keyed on `ApplicationEvent` itself.
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
