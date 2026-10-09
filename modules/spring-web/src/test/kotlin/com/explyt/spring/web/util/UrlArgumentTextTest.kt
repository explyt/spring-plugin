/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.toUElement

/**
 * The URL text a test request builder receives, read from the expression that builds it.
 *
 * A run-time part becomes [UrlArgumentText.VARIABLE_SEGMENT], a `{...}` template segment, so it matches exactly one
 * route segment the way a path variable does. `**` would read as a wildcard to a human and to an Ant matcher, so the
 * produced text is asserted as written rather than only through a route match.
 */
class UrlArgumentTextTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.kotlin_1_9_22)

    fun testTemplateEntriesBecomeVariableSegments() {
        assertEquals("/api/items/{*}/history/{*}", textOf("\"/api/items/\$id/history/\${page + 1}\""))
    }

    fun testConstantsStayAsWritten() {
        assertEquals("/api/items/42", textOf("\"/api/items/\" + 42"))
    }

    fun testConcatenatedVariableKeepsSingleSegmentRankingBeforeCaptureRest() {
        val request = textOf("\"/files/\" + id")
        assertEquals("/files/{*}", request)
        val matched = EndpointUrlMatcher.match(
            listOf("/files/{*path}", "/files/{id}"), request!!,
            EndpointUrlMatcher.Policy.REFERENCE, { it }, { null }
        )

        assertEquals(listOf("/files/{id}", "/files/{*path}"), matched.endpoints)
    }

    fun testUriFactoryIsUnwrapped() {
        assertEquals("http://localhost:8080/api/items/{*}?x=1", textOf("java.net.URI.create(\"http://localhost:8080/api/items/\$id?x=1\")"))
    }

    fun testUrlKnownOnlyAtRunTimeHasNoText() {
        assertNull(textOf("url"))
    }

    private fun textOf(argument: String): String? {
        myFixture.configureByText(
            "Probe.kt", """
            fun send(target: Any) {}

            fun probe(id: Long, page: Int, url: String) {
                send($argument)
            }
            """.trimIndent()
        )
        val probe = PsiTreeUtil.findChildrenOfType(myFixture.file, KtNamedFunction::class.java).single { it.name == "probe" }
        val call = PsiTreeUtil.findChildrenOfType(probe, KtCallExpression::class.java)
            .single { it.calleeExpression?.text == "send" }
        val uCall = call.toUElement() as UCallExpression
        assertNotNull("Precondition: the call resolves", uCall.resolve() as? PsiMethod)
        return UrlArgumentText.of(uCall.valueArguments.single())
    }
}
