/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.providers.EndpointIconGutterHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo.LineMarkerGutterIconRenderer
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.toUElementOfType

private const val TEST_DATA_PATH = "providers/linemarkers"
private const val REACTIVE_FUNCTION_PACKAGE = "org.springframework.web.reactive.function.server"

class RouterDslStringRouteMarkerTest : ExplytKotlinLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.kotlin_1_9_22
    )

    fun testCoRouterStringRouteHasAnAnyMethodMarker() {
        val file = configureRouter(
            "coRouter",
            """
            "/items" { ServerResponse.ok().buildAndAwait() }
            GET("/y") { ServerResponse.ok().buildAndAwait() }
            """
        )
        assertStringRouteResolvesToDslInvoke(file, "$REACTIVE_FUNCTION_PACKAGE.CoRouterFunctionDsl")

        myFixture.doHighlighting()

        assertEquals(listOf("/items" to emptyList(), "/y" to listOf("GET")), endpointMarkers())
        assertTrue(markerAnchors().all { it.firstChild == null })
    }

    fun testReactiveRouterStringRouteHasAnAnyMethodMarker() {
        val file = configureRouter(
            "router",
            """
            "/items" { ServerResponse.ok().build() }
            GET("/y") { ServerResponse.ok().build() }
            """
        )
        assertStringRouteResolvesToDslInvoke(file, "$REACTIVE_FUNCTION_PACKAGE.RouterFunctionDsl")

        myFixture.doHighlighting()

        assertEquals(listOf("/items" to emptyList(), "/y" to listOf("GET")), endpointMarkers())
        assertTrue(markerAnchors().all { it.firstChild == null })
    }

    fun testNestedStringRouteMarkerKeepsThePrefix() {
        val file = configureRouter(
            "coRouter",
            """"/api".nest { "/health" { ServerResponse.ok().buildAndAwait() } }"""
        )
        assertStringRouteResolvesToDslInvoke(file, "$REACTIVE_FUNCTION_PACKAGE.CoRouterFunctionDsl")

        myFixture.doHighlighting()

        assertEquals(listOf("/api/health" to emptyList<String>()), endpointMarkers())
    }

    fun testStringNestAloneHasNoAnyMethodMarker() {
        configureRouter("coRouter", """"/api".nest { GET("/x") { ServerResponse.ok().buildAndAwait() } }""")

        myFixture.doHighlighting()

        assertEquals(listOf("/api/x" to listOf("GET")), endpointMarkers())
    }

    fun testUserDefinedStringInvokeHasNoMarker() {
        val file = configureRouter(
            "coRouter",
            """
            "/x"(1)
            GET("/y") { ServerResponse.ok().buildAndAwait() }
            """,
            "operator fun String.invoke(times: Int) {}"
        )
        val method = stringRouteCall(file).resolve()!!
        assertEquals("invoke", method.name)
        assertFalse(method.containingClass?.qualifiedName.orEmpty().startsWith(REACTIVE_FUNCTION_PACKAGE))

        myFixture.doHighlighting()

        assertEquals(listOf("/y" to listOf("GET")), endpointMarkers())
    }

    private fun configureRouter(dsl: String, routes: String, declarations: String = ""): PsiFile =
        myFixture.configureByText(
            "ItemRouter.kt",
            """
            |import org.springframework.context.annotation.Bean
            |import org.springframework.context.annotation.Configuration
            |import org.springframework.web.reactive.function.server.ServerResponse
            |import org.springframework.web.reactive.function.server.buildAndAwait
            |import org.springframework.web.reactive.function.server.$dsl
            |
            |$declarations
            |
            |@Configuration
            |class ItemRouter {
            |
            |    @Bean
            |    fun itemsApi() = $dsl {
            |${routes.trimIndent().prependIndent("        ")}
            |    }
            |}
            """.trimMargin()
        )

    private fun stringRouteCall(file: PsiFile): UCallExpression =
        PsiTreeUtil.findChildrenOfType(file, KtCallExpression::class.java)
            .single { it.calleeExpression is KtStringTemplateExpression }
            .toUElementOfType<UCallExpression>()!!

    private fun assertStringRouteResolvesToDslInvoke(file: PsiFile, owner: String) {
        val call = stringRouteCall(file)
        assertEquals("java.lang.String", call.receiverType?.canonicalText)
        val method = call.resolve()
        assertNotNull("The \"/path\" { } route must resolve", method)
        assertEquals(owner, method!!.containingClass?.qualifiedName)
        assertEquals("invoke", method.name)
        assertEquals("void", method.returnType?.canonicalText)
    }

    private fun endpointRenderers() = myFixture.findAllGutters()
        .filter { it.icon == SpringIcons.ReadAccess }
        .filter { it.tooltipText == "Endpoint Actions" }
        .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }

    private fun markerAnchors() = endpointRenderers().mapNotNull { it.lineMarkerInfo.element }

    private fun endpointMarkers(): List<Pair<String, List<String>>> = endpointRenderers()
        .map { (it.lineMarkerInfo.navigationHandler as EndpointIconGutterHandler).endpointInfo }
        .map { it.path to it.requestMethods }
        .sortedBy { it.first }
}
