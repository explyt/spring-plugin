/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.intellij.codeInsight.daemon.GutterMark
import org.intellij.lang.annotations.Language
import javax.swing.Icon

class EventTypeMatchingTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.addClass("package com.example; public class BaseEvent {}")
        myFixture.addClass("package com.example; public class SubEvent extends BaseEvent {}")
        myFixture.addClass("package com.example; public class OtherEvent {}")
    }

    fun testListenerOfSuperEventFindsSubEventPublishCall() {
        addPublisher("events.publishEvent(new SubEvent());")

        assertEquals(listOf(SUB_CALL), publisherTargets("public void on(BaseEvent event) {}"))
    }

    fun testListenerOfSubEventDoesNotFindSuperEventPublishCall() {
        addPublisher("events.publishEvent(new BaseEvent());")

        assertEmpty(publisherTargets("public void on(SubEvent event) {}"))
    }

    fun testAnnotationClassesWinOverParameterTypeForPublishers() {
        addPublisher("events.publishEvent(new SubEvent());", "events.publishEvent(new OtherEvent());")

        assertEquals(
            listOf(SUB_CALL),
            publisherTargets("@EventListener(SubEvent.class) public void on(Object event) {}", annotated = false)
        )
    }

    fun testAnnotationClassesWinOverParameterTypeForListeners() {
        myFixture.addClass(listenerClass("@EventListener(SubEvent.class) public void on(Object event) {}", false))
        val publisher = addPublisher("events.publishEvent(new OtherEvent());")
        myFixture.configureFromExistingVirtualFile(publisher.containingFile.virtualFile)
        myFixture.doHighlighting()

        assertEmpty(SpringGutterTestUtil.getGutterTargetsStrings(singleGutter(SpringIcons.EventListener)))
    }

    fun testEmptyPublisherPopupDescribesTheSearch() {
        myFixture.configureByText("Listener.java", listenerClass("public void on(OtherEvent event) {}", true))
        myFixture.doHighlighting()
        val gutter = singleGutter(SpringIcons.EventPublisher)

        assertEmpty(SpringGutterTestUtil.getGutterTargetsStrings(gutter))
        assertEquals(
            SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.event.publisher"),
            SpringGutterTestUtil.getGutterEmptyText(gutter)
        )
        assertEquals(
            "No event publisher found in this application or its library sources",
            SpringGutterTestUtil.getGutterEmptyText(gutter)
        )
    }

    private fun addPublisher(vararg calls: String) = myFixture.addClass(
        """
        package com.example;

        import org.springframework.context.ApplicationEventPublisher;

        public class Publisher {
            void fire(ApplicationEventPublisher events) {
                ${calls.joinToString("\n                ")}
            }
        }
        """.trimIndent()
    )

    private fun publisherTargets(method: String, annotated: Boolean = true): List<String> {
        myFixture.configureByText("Listener.java", listenerClass(method, annotated))
        myFixture.doHighlighting()
        return SpringGutterTestUtil.getGutterTargetsStrings(singleGutter(SpringIcons.EventPublisher))
    }

    private fun singleGutter(icon: Icon): GutterMark {
        val gutters = myFixture.findAllGutters().filter { it.icon == icon }
        assertEquals("exactly one gutter with the expected icon", 1, gutters.size)
        return gutters.single()
    }

    @Language("JAVA")
    private fun listenerClass(method: String, annotated: Boolean): String =
        """
        package com.example;

        import org.springframework.context.event.EventListener;
        import org.springframework.stereotype.Component;

        @Component
        public class Listener {
            ${if (annotated) "@EventListener " else ""}$method
        }
        """.trimIndent()

    private companion object {
        const val SUB_CALL = "events.publishEvent(new SubEvent())"
    }
}
