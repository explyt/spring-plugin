/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.intellij.codeInsight.daemon.GutterMark
import javax.swing.Icon

private const val PLATFORM_DEFAULT_GROUP = "XML"

class NavigationGroupTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1
    )

    fun testBeanTargetsAreGroupedAsSpringBean() {
        myFixture.configureByText(
            "FooComponent.kt",
            """
                import org.springframework.stereotype.Component

                @Component
                class Foo

                @Component
                class Bar(private val foo: Foo)
            """.trimIndent()
        )
        myFixture.doHighlighting()

        assertGroups(setOf("Spring Bean"), SpringIcons.SpringBeanDependencies)
    }

    fun testEventPublisherAndListenerTargetsAreGroupedByEventRole() {
        myFixture.configureByText(
            "Events.kt",
            """
                import org.springframework.context.ApplicationEventPublisher
                import org.springframework.context.event.EventListener
                import org.springframework.stereotype.Component

                class OrderEvent

                @Component
                class OrderListener {
                    @EventListener
                    fun onOrder(event: OrderEvent) {
                    }
                }

                @Component
                class OrderService(private val publisher: ApplicationEventPublisher) {
                    fun place() {
                        publisher.publishEvent(OrderEvent())
                    }
                }
            """.trimIndent()
        )
        myFixture.doHighlighting()

        assertGroups(setOf("Event Publisher"), SpringIcons.EventPublisher)
        assertGroups(setOf("Event Listener"), SpringIcons.EventListener)
    }

    fun testConfigurationPropertyTargetsAreGroupedAsConfigurationProperty() {
        myFixture.addFileToProject("application.properties", "configuration.value=1")
        myFixture.configureByText(
            "MainPropertiesConfiguration.kt",
            """
                import org.springframework.boot.context.properties.ConfigurationProperties
                import org.springframework.context.annotation.Configuration

                @ConfigurationProperties(prefix="configuration")
                @Configuration
                open class MainPropertiesConfiguration {
                    var value: String? = null
                }
            """.trimIndent()
        )
        myFixture.doHighlighting()

        assertGroups(setOf("Configuration Property"), SpringIcons.SpringSetting)
    }

    private fun assertGroups(expected: Set<String>, icon: Icon) {
        val gutters = myFixture.findAllGutters().filter { it.icon == icon }
        assertTrue("The fixture must produce at least one gutter for $icon", gutters.isNotEmpty())

        val groups = gutters.flatMapTo(mutableSetOf()) { groupsOf(it) }
        assertFalse(
            "Targets must not be grouped under the platform's default XML group, but were: $groups",
            groups.contains(PLATFORM_DEFAULT_GROUP)
        )
        assertEquals(expected, groups)
    }

    private fun groupsOf(gutter: GutterMark): Set<String> {
        val groups = SpringGutterTestUtil.getGutterTargetGroups(gutter)
        assertTrue("A navigable gutter must declare a navigation group", groups.isNotEmpty())
        return groups
    }
}
