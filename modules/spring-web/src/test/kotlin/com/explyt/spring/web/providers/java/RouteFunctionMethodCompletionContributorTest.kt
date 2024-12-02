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

package com.explyt.spring.web.providers.java

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.LineMarkerInfo.LineMarkerGutterIconRenderer
import junit.framework.TestCase

private const val TEST_DATA_PATH = "providers/linemarkers"

class RouteFunctionEndpointLineMarkerProviderTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springTest_6_0_7
    )

    fun testLineMarkerMvc_Get() {
        myFixture.configureByFiles("UserDataMvcTest.java", "UserHandler.java")
        myFixture.configureByText(
            "UserRouter.java", """
                    import static org.springframework.http.MediaType.APPLICATION_JSON;
                    import static org.springframework.web.reactive.function.server.RequestPredicates.*;
                    import static org.springframework.web.reactive.function.server.RouterFunctions.route;
                    
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.stereotype.Component;
                    
                    @Configuration
                    public class UserRouter {

                        private static final RequestPredicate ACCEPT_JSON = accept(APPLICATION_JSON);

                        @Autowired
                        private UserHandler userHandler;

                        @Bean
                        public RouterFunction<ServerResponse> differentFunction() {
                            return route()
                                    .GET("/users/{userId}", ACCEPT_JSON, userHandler::getUserCustomers) //some
                                    .build();
                        }
                    }
                """.trimIndent()
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

    fun testLineMarkerMvc_Post() {
        myFixture.configureByFiles("UserDataMvcTest.java", "UserHandler.java")
        myFixture.configureByText(
            "UserRouter.java", """
                    import static org.springframework.http.MediaType.APPLICATION_JSON;
                    import static org.springframework.web.reactive.function.server.RequestPredicates.*;
                    import static org.springframework.web.reactive.function.server.RouterFunctions.route;
                    
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.stereotype.Component;
                    
                    @Configuration
                    public class UserRouter {

                        private static final RequestPredicate ACCEPT_JSON = accept(APPLICATION_JSON);

                        @Autowired
                        private UserHandler userHandler;

                        @Bean
                        public RouterFunction<ServerResponse> differentFunction() {
                            return route()
                                    .POST("/users/customers", ACCEPT_JSON, userHandler::getUserCustomers)
                                    .build();
                        }
                    }
                """.trimIndent()
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

    fun testLineMarkerMvc_Delete() {
        myFixture.configureByFiles("UserDataMvcTest.java", "UserHandler.java")
        myFixture.configureByText(
            "UserRouter.java", """
                    import static org.springframework.http.MediaType.APPLICATION_JSON;
                    import static org.springframework.web.reactive.function.server.RequestPredicates.*;
                    import static org.springframework.web.reactive.function.server.RouterFunctions.route;
                    
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.stereotype.Component;
                    
                    @Configuration
                    public class UserRouter {

                        private static final RequestPredicate ACCEPT_JSON = accept(APPLICATION_JSON);

                        @Autowired
                        private UserHandler userHandler;

                        @Bean
                        public RouterFunction<ServerResponse> differentFunction() {
                            return route()
                                    .DELETE("/users/{userId}", ACCEPT_JSON, userHandler::getUserCustomers)
                                    .build();
                        }
                    }
                """.trimIndent()
        )
        myFixture.doHighlighting()

        val lineMarkers = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
            .filter { it.tooltipText == "Endpoint Actions" }

        assertEquals(1, lineMarkers.size)

        TestCase.assertEquals(
            "DELETE",
            lineMarkers
                .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }
                .map { it.lineMarkerInfo.element?.text }
                .firstOrNull()
        )
    }

    fun testLineMarkerWebTestClient_Get() {
        myFixture.configureByFiles("UserDataWebTestClient.java", "UserHandler.java")
        myFixture.configureByText(
            "UserRouter.java", """
                    import static org.springframework.http.MediaType.APPLICATION_JSON;
                    import static org.springframework.web.reactive.function.server.RequestPredicates.*;
                    import static org.springframework.web.reactive.function.server.RouterFunctions.route;
                    
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.stereotype.Component;

                    @Configuration
                    public class UserRouter {

                        private static final RequestPredicate ACCEPT_JSON = accept(APPLICATION_JSON);

                        @Autowired
                        private UserHandler userHandler;

                        @Bean
                        public RouterFunction<ServerResponse> getFunction() {
                            return route()
                                    .GET("/users/{userId}", ACCEPT_JSON, userHandler::getUserCustomers)
                                    .build();
                        }
                    }
                """.trimIndent()
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

    fun testLineMarkerWebTestClient_Post() {
        myFixture.configureByFiles("UserDataWebTestClient.java", "UserHandler.java")
        myFixture.configureByText(
            "UserRouter.java", """
                    import static org.springframework.http.MediaType.APPLICATION_JSON;
                    import static org.springframework.web.reactive.function.server.RequestPredicates.*;
                    import static org.springframework.web.reactive.function.server.RouterFunctions.route;
                    
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.stereotype.Component;

                    @Configuration
                    public class UserRouter {

                        private static final RequestPredicate ACCEPT_JSON = accept(APPLICATION_JSON);

                        @Autowired
                        private UserHandler userHandler;

                        @Bean
                        public RouterFunction<ServerResponse> getFunction() {
                            return route()
                                    .POST("/users/{userId}", ACCEPT_JSON, userHandler::getUserCustomers)
                                    .build();
                        }
                    }
                """.trimIndent()
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

    fun testLineMarkerWebTestClient_Delete() {
        myFixture.configureByFiles("UserDataWebTestClient.java", "UserHandler.java")
        myFixture.configureByText(
            "UserRouter.java", """
                    import static org.springframework.http.MediaType.APPLICATION_JSON;
                    import static org.springframework.web.reactive.function.server.RequestPredicates.*;
                    import static org.springframework.web.reactive.function.server.RouterFunctions.route;
                    
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.stereotype.Component;

                    @Configuration
                    public class UserRouter {

                        private static final RequestPredicate ACCEPT_JSON = accept(APPLICATION_JSON);

                        @Autowired
                        private UserHandler userHandler;

                        @Bean
                        public RouterFunction<ServerResponse> getFunction() {
                            return route()
                                    .DELETE("/users/{userId}", ACCEPT_JSON, userHandler::getUserCustomers)
                                    .build();
                        }
                    }
                """.trimIndent()
        )
        myFixture.doHighlighting()

        val lineMarkers = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
            .filter { it.tooltipText == "Endpoint Actions" }

        assertEquals(1, lineMarkers.size)

        TestCase.assertEquals(
            "DELETE",
            lineMarkers
                .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }
                .map { it.lineMarkerInfo.element?.text }
                .firstOrNull()
        )
    }
}