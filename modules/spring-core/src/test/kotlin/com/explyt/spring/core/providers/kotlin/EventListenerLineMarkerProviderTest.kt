/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

private const val TEST_DATA_PATH = "providers/linemarkers"

@TestMetadata(TEST_DATA_PATH)
class EventListenerLineMarkerProviderTest : ExplytKotlinLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springTx_6_0_7
    )

    fun testEventListenerLineMarker() {
        val vf = myFixture.copyFileToProject(
            "EventListener.kt"
        )

        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allEventGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.EventPublisher || it.icon == SpringIcons.EventListener }
        val gutterTargetString = allEventGutters.map { SpringGutterTestUtil.getGutterTargetsStrings(it) }
        assertTrue(allEventGutters.isNotEmpty())
        for (targets in gutterTargetString) {
            assertTrue(targets.isNotEmpty())
        }
    }

    fun testTransactionalEventListenerLineMarker() {

        @Language("kotlin") val string = """
            package com.example
            
            import org.springframework.context.ApplicationEventPublisher
            import org.springframework.stereotype.Component
            import org.springframework.transaction.event.TransactionalEventListener
            
            @Component
            class EventPublisher(private val eventPublisher: ApplicationEventPublisher) {
                
                fun publishEvent(message: String) {
                    eventPublisher.publishEvent(CustomEvent(message))
                }

                @TransactionalEventListener
                fun handleCustomEventDefault(event: CustomEvent) {
                    println("Handling event with default phase: ${'$'}{event.message}")
                }
            }

            data class CustomEvent(val message: String)
            """
        myFixture.configureByText(
            "EventPublisher.kt",
            string.trimIndent()
        )

        myFixture.doHighlighting()

        val allEventGutters = myFixture.findAllGutters().filter { it.icon == SpringIcons.EventListener }

        assertTrue("Expected to find event gutters", allEventGutters.isNotEmpty())

        // Verify that gutters have targets
        val gutterTargetString = allEventGutters.map { SpringGutterTestUtil.getGutterTargetsStrings(it) }
        for (targets in gutterTargetString) {
            assertTrue("Expected gutter to have targets", targets.isNotEmpty())
        }
    }

}
