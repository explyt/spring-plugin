/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.providers.EndpointIconGutterHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo.LineMarkerGutterIconRenderer
import com.intellij.psi.util.PsiTreeUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.toUElementOfType

private const val TEST_DATA_PATH = "providers/linemarkers"

class RouteFunctionEndpointLineMarkerProviderTest : ExplytKotlinLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springTest_6_0_7
    )

    fun testLineMarkerWebTestClient_Post() {
        myFixture.configureByFiles("UserRouterTest.kt", "UserHandler.kt")
        myFixture.configureByText(
            "UserRouter.kt", """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.coRouter

            @Configuration
            class UserRouter(private val handler: UserHandler) {

                @Bean
                fun usersApi() = coRouter {
                    "/api/users".nest {
                        POST("", handler::createUser)
                    }
                }
            }
            """
        )
        myFixture.doHighlighting()

        val lineMarkers = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
            .filter { it.tooltipText == "Endpoint Actions" }

        assertEquals(1, lineMarkers.size)

        TestCase.assertEquals(
            "POST",
            lineMarkers
                .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }
                .map { it.lineMarkerInfo.element?.text }
                .firstOrNull()
        )
    }

    fun testLineMarkerWebTestClient_GET() {
        myFixture.configureByFiles("UserRouterTest.kt", "UserHandler.kt")
        myFixture.configureByText(
            "UserRouter.kt", """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.coRouter

            @Configuration
            class UserRouter(private val handler: UserHandler) {

                @Bean
                fun usersApi() = coRouter {
                    "/api/users".nest {
                        "/{id}".nest {
                            GET("", handler::getUserById)
                        }
                    }
                }
            }
            """
        )
        myFixture.doHighlighting()

        val lineMarkers = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
            .filter { it.tooltipText == "Endpoint Actions" }

        assertEquals(1, lineMarkers.size)

        TestCase.assertEquals(
            "GET",
            lineMarkers
                .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }
                .map { it.lineMarkerInfo.element?.text }
                .firstOrNull()
        )
    }

    fun testLineMarkerWebTestClient_Patch() {
        myFixture.configureByFiles("UserRouterTest.kt", "UserHandler.kt")
        myFixture.configureByText(
            "UserRouter.kt", """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.coRouter

            @Configuration
            class UserRouter(private val handler: UserHandler) {

                @Bean
                fun usersApi() = coRouter {
                    "/api/users".nest {
                        "/{id}".nest {
                            PATCH("", handler::updateUserData)
                        }
                    }
                }
            }
            """
        )
        myFixture.doHighlighting()

        val lineMarkers = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
            .filter { it.tooltipText == "Endpoint Actions" }

        assertEquals(1, lineMarkers.size)

        TestCase.assertEquals(
            "PATCH",
            lineMarkers
                .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }
                .map { it.lineMarkerInfo.element?.text }
                .firstOrNull()
        )
    }

    fun testTwoArgumentPathRouteHasAnAnyMethodMarker() {
        myFixture.configureByFiles("UserHandler.kt")
        val file = myFixture.configureByText(
            "UserRouter.kt", """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.coRouter

            @Configuration
            class UserRouter(private val handler: UserHandler) {

                @Bean
                fun usersApi() = coRouter {
                    path("/x", handler::getUserById)
                    GET("/y", handler::getUserById)
                }
            }
            """
        )
        val pathRoute = PsiTreeUtil.findChildrenOfType(file, KtCallExpression::class.java)
            .single { it.calleeExpression?.text == "path" }
            .toUElementOfType<UCallExpression>()!!
        val method = pathRoute.resolve()!!
        assertEquals(
            "org.springframework.web.reactive.function.server.CoRouterFunctionDsl",
            method.containingClass?.qualifiedName
        )
        assertEquals("void", method.returnType?.canonicalText)
        assertEquals(2, pathRoute.valueArgumentCount)
        assertEquals(2, method.parameterList.parametersCount)
        assertEquals("java.lang.String", method.parameterList.parameters.first().type.canonicalText)

        myFixture.doHighlighting()

        assertEquals(
            listOf(
                Triple("path", "/x", emptyList<String>()),
                Triple("GET", "/y", listOf("GET"))
            ),
            endpointMarkers()
        )
    }

    fun testAnyMethodPathRouteMarkerDoesNotOfferAnOpenApiDescription() {
        myFixture.configureByFiles("UserHandler.kt")
        myFixture.configureByText(
            "UserRouter.kt", """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.coRouter

            @Configuration
            class UserRouter(private val handler: UserHandler) {

                @Bean
                fun usersApi() = coRouter {
                    path("/x", handler::getUserById)
                    GET("/y", handler::getUserById)
                }
            }
            """
        )
        myFixture.doHighlighting()

        val offersByPath = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
            .filter { it.tooltipText == "Endpoint Actions" }
            .mapNotNull { (it as? LineMarkerGutterIconRenderer<*>)?.lineMarkerInfo?.navigationHandler }
            .map { it as EndpointIconGutterHandler }
            .associate { it.endpointInfo.path to it.offersOpenApiDescription }

        assertEquals(mapOf("/x" to false, "/y" to true), offersByPath)
    }

    fun testTwoArgumentPathCallOutsideTheDslHasNoMarker() {
        myFixture.configureByFiles("UserHandler.kt")
        val file = myFixture.configureByText(
            "UserRouter.kt", """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.coRouter

            object NonDsl { fun path(pattern: String, handler: Any) {} }

            @Configuration
            class UserRouter(private val handler: UserHandler) {

                @Bean
                fun usersApi() = coRouter {
                    NonDsl.path("/x", handler::getUserById)
                    GET("/y", handler::getUserById)
                }
            }
            """
        )
        val nonDslPath = PsiTreeUtil.findChildrenOfType(file, KtCallExpression::class.java)
            .single { it.calleeExpression?.text == "path" }
            .toUElementOfType<UCallExpression>()!!
        val method = nonDslPath.resolve()!!
        assertEquals("NonDsl", method.containingClass?.qualifiedName)
        assertEquals("void", method.returnType?.canonicalText)
        assertEquals(2, nonDslPath.valueArgumentCount)
        assertEquals(2, method.parameterList.parametersCount)
        assertEquals("java.lang.String", method.parameterList.parameters.first().type.canonicalText)

        myFixture.doHighlighting()

        assertEquals(listOf(Triple("GET", "/y", listOf("GET"))), endpointMarkers())
    }

    private fun endpointMarkers(): List<Triple<String?, String, List<String>>> = myFixture.findAllGutters()
        .filter { it.icon == SpringIcons.ReadAccess }
        .filter { it.tooltipText == "Endpoint Actions" }
        .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }
        .map { renderer ->
            val endpointInfo = (renderer.lineMarkerInfo.navigationHandler as EndpointIconGutterHandler).endpointInfo
            Triple(renderer.lineMarkerInfo.element?.text, endpointInfo.path, endpointInfo.requestMethods)
        }
        .sortedBy { it.second }
}
