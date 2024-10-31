package com.esprito.spring.web.providers.java

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.test.ExplytJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.test.util.SpringGutterTestUtil
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
                                    .GET("/users/{userId}", ACCEPT_JSON, userHandler::getUserCustomers)
                                    .build();
                        }
                    }
                """.trimIndent()
        )
        myFixture.doHighlighting()

        val allEndpointUsageGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
        TestCase.assertTrue(allEndpointUsageGutters.isNotEmpty())

        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allEndpointUsageGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "get(\"/users/{userId}\", 1)" }
        }.size, 1)
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

        val allEndpointUsageGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
        TestCase.assertTrue(allEndpointUsageGutters.isNotEmpty())

        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allEndpointUsageGutters)

        assertEquals(gutterTargetString.size, 0)
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

        val allEndpointUsageGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
        TestCase.assertTrue(allEndpointUsageGutters.isNotEmpty())

        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allEndpointUsageGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "delete(\"/users/{userId}\", 1)" }
        }.size, 1)
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

        val allEndpointUsageGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
        TestCase.assertTrue(allEndpointUsageGutters.isNotEmpty())

        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allEndpointUsageGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("get()") && it.contains("/users/{userId}") }
        }.size, 1)
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

        val allEndpointUsageGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
        TestCase.assertTrue(allEndpointUsageGutters.isNotEmpty())

        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allEndpointUsageGutters)

        assertEquals(gutterTargetString.size, 0)
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

        val allEndpointUsageGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.ReadAccess }
        TestCase.assertTrue(allEndpointUsageGutters.isNotEmpty())

        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allEndpointUsageGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("delete()") && it.contains("/users/{userId}") }
        }.size, 1)
    }

}