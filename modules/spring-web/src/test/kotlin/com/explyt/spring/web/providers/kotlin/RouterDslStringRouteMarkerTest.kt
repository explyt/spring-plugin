/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
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

    fun testConstantReceiverRouteHasAnAnyMethodMarker() {
        val file = configureRouter(
            "coRouter",
            """
            ITEMS { ServerResponse.ok().buildAndAwait() }
            GET("/y") { ServerResponse.ok().buildAndAwait() }
            """,
            """const val ITEMS = "/items""""
        )
        assertNonLiteralReceiverRouteResolvesToDslInvoke(file)

        myFixture.doHighlighting()

        assertEquals(listOf("/items" to emptyList(), "/y" to listOf("GET")), endpointMarkers())
        val itemsAnchor = endpointRenderers()
            .single { (it.lineMarkerInfo.navigationHandler as EndpointIconGutterHandler).endpointInfo.path == "/items" }
            .lineMarkerInfo.element!!
        assertNull(itemsAnchor.firstChild)
        assertEquals("ITEMS", itemsAnchor.text)
    }

    fun testNestedConstantReceiverRouteMarkerKeepsThePrefix() {
        val file = configureRouter(
            "coRouter",
            """"/api".nest { ITEMS { ServerResponse.ok().buildAndAwait() } }""",
            """const val ITEMS = "/items""""
        )
        assertNonLiteralReceiverRouteResolvesToDslInvoke(file)

        myFixture.doHighlighting()

        assertEquals(listOf("/api/items" to emptyList<String>()), endpointMarkers())
        assertTrue(markerAnchors().all { it.firstChild == null })
    }

    fun testLocalValReceiverRouteMarkerAgreesWithTheEndpointModel() {
        val file = configureRouter(
            "coRouter",
            """
            val p = "/x"
            p { ServerResponse.ok().buildAndAwait() }
            GET("/y") { ServerResponse.ok().buildAndAwait() }
            """
        )
        assertNonLiteralReceiverRouteResolvesToDslInvoke(file)

        myFixture.doHighlighting()

        assertEquals(webFluxEndpoints(), endpointMarkers())
        assertTrue(("/y" to listOf("GET")) in webFluxEndpoints())
    }

    fun testUnresolvableStringReceiverRouteHasNoEndpointAndNoMarker() {
        val file = myFixture.configureByText(
            "ItemRouter.kt",
            """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.ServerResponse
            import org.springframework.web.reactive.function.server.buildAndAwait
            import org.springframework.web.reactive.function.server.coRouter

            @Configuration
            class ItemRouter {

                @Bean
                fun itemsApi(prefix: String) = coRouter {
                    prefix { ServerResponse.ok().buildAndAwait() }
                    GET("/y") { ServerResponse.ok().buildAndAwait() }
                }
            }
            """.trimIndent()
        )
        assertNonLiteralReceiverRouteResolvesToDslInvoke(file)

        myFixture.doHighlighting()

        assertEquals(listOf("/y" to listOf("GET")), webFluxEndpoints())
        assertEquals(listOf("/y" to listOf("GET")), endpointMarkers())
    }

    private fun assertNonLiteralReceiverRouteResolvesToDslInvoke(file: PsiFile) {
        val call = PsiTreeUtil.findChildrenOfType(file, KtCallExpression::class.java)
            .mapNotNull { it.toUElementOfType<UCallExpression>() }
            .single { it.methodName == "invoke" }
        assertFalse((call.sourcePsi as KtCallExpression).calleeExpression is KtStringTemplateExpression)
        assertEquals("java.lang.String", call.receiverType?.canonicalText)
        val method = call.resolve()
        assertNotNull("${call.sourcePsi?.text} must resolve", method)
        assertEquals("$REACTIVE_FUNCTION_PACKAGE.CoRouterFunctionDsl", method!!.containingClass?.qualifiedName)
        assertEquals("invoke", method.name)
        assertEquals("void", method.returnType?.canonicalText)
    }

    private fun webFluxEndpoints(): List<Pair<String, List<String>>> = SpringWebEndpointsLoader.EP_NAME
        .getExtensions(project)
        .filter { it.getType() == EndpointType.SPRING_WEBFLUX }
        .filter { it.isApplicable(module) }
        .flatMap { it.searchEndpoints(module) }
        .map { it.path to it.requestMethods }
        .sortedBy { it.first }

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
