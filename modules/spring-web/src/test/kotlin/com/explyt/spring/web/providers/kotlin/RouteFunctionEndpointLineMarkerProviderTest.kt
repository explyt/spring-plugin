/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.LineMarkerInfo.LineMarkerGutterIconRenderer
import junit.framework.TestCase

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

}