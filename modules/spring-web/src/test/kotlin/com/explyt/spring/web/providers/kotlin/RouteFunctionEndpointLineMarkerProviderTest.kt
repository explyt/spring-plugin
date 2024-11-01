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

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
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

        val allEndpointUsageGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
        TestCase.assertTrue(allEndpointUsageGutters.isNotEmpty())

        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allEndpointUsageGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "uri(\"/api/users\")" }
        }.size, 1)
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

        val allEndpointUsageGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
        TestCase.assertTrue(allEndpointUsageGutters.isNotEmpty())

        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allEndpointUsageGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "uri(\"/api/users/123\")" }
        }.size, 1)
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

        val allEndpointUsageGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
        TestCase.assertTrue(allEndpointUsageGutters.isNotEmpty())

        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allEndpointUsageGutters)

        assertEquals(gutterTargetString.size, 0)
    }

}