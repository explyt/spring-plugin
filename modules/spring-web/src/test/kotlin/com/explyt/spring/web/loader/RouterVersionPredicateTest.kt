/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.toUElementOfType

class RouterVersionPredicateTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary("org.springframework:spring-webmvc:7.0.9"),
        TestLibrary("org.springframework:spring-webflux:7.0.9"),
        TestLibrary("org.springframework:spring-context:7.0.9"),
        TestLibrary("org.springframework.boot:spring-boot:3.1.1", false),
        TestLibrary.kotlin_1_9_22
    )

    fun testCoRouterVersionPredicateKeepsNestedRoutes() {
        assertDslRoutes(EndpointType.SPRING_WEBFLUX, "coRouter")
    }

    fun testReactiveRouterVersionPredicateKeepsNestedRoutes() {
        assertDslRoutes(EndpointType.SPRING_WEBFLUX, "router")
    }

    fun testServletRouterVersionPredicateKeepsNestedRoutes() {
        assertDslRoutes(EndpointType.SPRING_MVC, "router")
    }

    fun testReactiveBuilderVersionPredicateKeepsNestedRoutes() {
        assertBuilderRoutes(EndpointType.SPRING_WEBFLUX)
    }

    fun testServletBuilderVersionPredicateKeepsNestedRoutes() {
        assertBuilderRoutes(EndpointType.SPRING_MVC)
    }

    private fun assertDslRoutes(type: EndpointType, dsl: String) {
        val functionPackage = functionPackage(type)
        val file = myFixture.addFileToProject(
            "VersionRouterConfig.kt",
            """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import $functionPackage.RequestPredicates
            import $functionPackage.RouterFunction
            import $functionPackage.ServerRequest
            import $functionPackage.ServerResponse
            import $functionPackage.$dsl

            @Configuration
            class VersionRouterConfig {
                @Bean
                fun routes(): RouterFunction<ServerResponse> = $dsl {
                    RequestPredicates.version("1.1").nest {
                        GET("/b") { _: ServerRequest -> error("Unused handler") }
                    }
                    GET("/s") { _: ServerRequest -> error("Unused handler") }
                }
            }
            """.trimIndent()
        )
        val version = PsiTreeUtil.findChildrenOfType(file, KtCallExpression::class.java)
            .single { it.calleeExpression?.text == "version" }
            .toUElementOfType<UCallExpression>()!!
        val method = version.resolve()
        assertNotNull("Spring 7 version predicate must resolve", method)
        assertEquals("$functionPackage.RequestPredicates", method!!.containingClass?.qualifiedName)
        assertEquals("$functionPackage.RequestPredicate", method.returnType?.canonicalText)
        assertEquals(listOf("java.lang.Object"), method.parameterList.parameters.map { it.type.canonicalText })
        assertNestedAndSiblingRoutes(type)
    }

    private fun assertBuilderRoutes(type: EndpointType) {
        val functionPackage = functionPackage(type)
        val file = myFixture.addFileToProject(
            "VersionBuilderRouterConfig.java",
            """
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import $functionPackage.RequestPredicates;
            import $functionPackage.RouterFunction;
            import $functionPackage.RouterFunctions;
            import $functionPackage.ServerResponse;

            @Configuration
            public class VersionBuilderRouterConfig {
                @Bean
                public RouterFunction<ServerResponse> routes() {
                    return RouterFunctions.route()
                        .nest(RequestPredicates.version("1.1"), b -> b.GET("/b", request -> null))
                        .GET("/s", request -> null)
                        .build();
                }
            }
            """.trimIndent()
        )
        val version = PsiTreeUtil.findChildrenOfType(file, PsiMethodCallExpression::class.java)
            .single { it.methodExpression.referenceName == "version" }
        val method = version.resolveMethod()
        assertNotNull("Spring 7 version predicate must resolve", method)
        assertEquals("$functionPackage.RequestPredicates", method!!.containingClass?.qualifiedName)
        assertEquals("$functionPackage.RequestPredicate", method.returnType?.canonicalText)
        assertEquals(listOf("java.lang.Object"), method.parameterList.parameters.map { it.type.canonicalText })
        assertNestedAndSiblingRoutes(type)
    }

    private fun assertNestedAndSiblingRoutes(type: EndpointType) {
        val loader = SpringWebEndpointsLoader.EP_NAME.getExtensions(project)
            .filterIsInstance<FunctionalRouteEndpointsLoader>()
            .single { it.getType() == type }
        assertTrue("The $type loader must be applicable", loader.isApplicable(module))
        val routes = loader.searchEndpoints(module)
            .map { it.path to it.requestMethods.single() }
            .sortedBy { it.first }
            .toList()
        assertEquals(listOf("/b" to "GET", "/s" to "GET"), routes)
    }

    private fun functionPackage(type: EndpointType): String = when (type) {
        EndpointType.SPRING_MVC -> "org.springframework.web.servlet.function"
        EndpointType.SPRING_WEBFLUX -> "org.springframework.web.reactive.function.server"
        else -> error("Unsupported router stack: $type")
    }
}
