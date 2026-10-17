/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.explyt.spring.web.util.RoutePathResolver
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.toUElementOfType

private const val BUILDER = "org.springframework.web.servlet.function.RouterFunctions.Builder"

class RouterBuilderPrefixCapTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.kotlin_1_9_22
    )

    fun testNestedPathPrefixCombinationsAboveTheCapAreNotListed() {
        addPaths(prefixCount = 11, pathCount = 10)
        addRouterConfig(
            """route().GET("/s", h::list).path(Paths.PREFIXES, b -> b.path(Paths.PATHS, c -> c.GET("/x", h::list))).build()"""
        )
        assertPathArgumentValues("Paths.PREFIXES", 11)
        assertPathArgumentValues("Paths.PATHS", 10)

        assertEquals(listOf("/s" to "GET"), servletEndpoints())
    }

    fun testNestedPathPrefixCombinationsAtTheCapAreAllListed() {
        addPaths(prefixCount = 10, pathCount = 10)
        addRouterConfig(
            """route().path(Paths.PREFIXES, b -> b.path(Paths.PATHS, c -> c.GET("/x", h::list))).build()"""
        )
        assertPathArgumentValues("Paths.PREFIXES", 10)
        assertPathArgumentValues("Paths.PATHS", 10)

        val expected = (1..10).flatMap { prefix -> (1..10).map { path -> "/p$prefix/r$path/x" to "GET" } }
            .sortedBy { it.first + it.second }
        assertEquals(expected, servletEndpoints())
    }

    private fun assertPathArgumentValues(argumentText: String, expectedCount: Int) {
        val config = myFixture.psiManager.findFile(myFixture.findFileInTempDir("CappedRouterConfig.java"))!!
        val call = PsiTreeUtil.findChildrenOfType(config, PsiMethodCallExpression::class.java)
            .single {
                it.methodExpression.referenceName == "path" &&
                        it.argumentList.expressions.firstOrNull()?.text == argumentText
            }
        assertEquals(call.text, BUILDER, call.resolveMethod()?.containingClass?.qualifiedName)
        val argument = call.argumentList.expressions.first().toUElementOfType<UExpression>()!!
        assertEquals(call.text, expectedCount, RoutePathResolver.resolveUriValues(argument).size)
    }

    private fun addPaths(prefixCount: Int, pathCount: Int) {
        myFixture.addFileToProject(
            "Paths.kt",
            """
            object Paths {
                @JvmField val PREFIXES = listOf(${(1..prefixCount).joinToString { "\"/p$it\"" }})
                @JvmField val PATHS = listOf(${(1..pathCount).joinToString { "\"/r$it\"" }})
            }
            """.trimIndent()
        )
    }

    private fun addRouterConfig(route: String) {
        myFixture.addFileToProject(
            "CappedRouterConfig.java",
            """
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.web.servlet.function.RouterFunction;
            import org.springframework.web.servlet.function.RouterFunctions;
            import org.springframework.web.servlet.function.ServerRequest;
            import org.springframework.web.servlet.function.ServerResponse;

            @Configuration
            public class CappedRouterConfig {

                @Bean
                public RouterFunction<ServerResponse> routes(Handler h) {
                    return RouterFunctions.$route;
                }

                public interface Handler {
                    ServerResponse list(ServerRequest request);
                }
            }
            """.trimIndent()
        )
    }

    private fun servletEndpoints(): List<Pair<String, String>> = SpringWebEndpointsLoader.EP_NAME
        .getExtensions(module.project).asSequence()
        .filter { it.getType() == EndpointType.SPRING_MVC }
        .filter { it.isApplicable(module) }
        .flatMap { it.searchEndpoints(module) }
        .map(::toPathAndMethod)
        .sortedBy { it.first + it.second }
        .toList()

    private fun toPathAndMethod(endpoint: EndpointElement) = endpoint.path to endpoint.requestMethods.joinToString()
}
