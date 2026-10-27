/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web

import com.explyt.base.LibraryClassCache
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier

private const val CO_ROUTER_FUNCTION_DSL = "org.springframework.web.reactive.function.server.CoRouterFunctionDsl"

/**
 * The defect behind #416 was a plugin-side verb list that had fallen behind the router DSL: Spring declared seven
 * route methods and the plugin recognised five. A test comparing the constant against a list spelled out in the test
 * cannot detect that, because both sides are maintained by the same hand and drift together — so the DSL's route
 * methods are read from the library class itself.
 */
class SpringWebHttpVerbsTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.springBoot_3_1_1
    )

    fun testEveryRouteMethodDeclaredByTheRouterDslIsRecognised() {
        val declaredByTheDsl = routeMethodNamesOf(coRouterFunctionDsl())

        assertTrue(
            "no route methods were read from $CO_ROUTER_FUNCTION_DSL — the assertion below would pass vacuously",
            declaredByTheDsl.isNotEmpty()
        )
        assertTrue(
            "the route methods read from $CO_ROUTER_FUNCTION_DSL look wrong: $declaredByTheDsl",
            "GET" in declaredByTheDsl
        )

        assertTrue(
            "missing: ${declaredByTheDsl - SpringWebClasses.ROUTER_DSL_ROUTE_METHODS.toSet()}",
            SpringWebClasses.ROUTER_DSL_ROUTE_METHODS.containsAll(declaredByTheDsl)
        )
    }

    fun testGenericMethodRouteIsRecognisedAlongsideTheVerbNamedOnes() {
        val genericMethod = SpringWebClasses.ROUTER_DSL_GENERIC_METHOD

        assertTrue(
            "$genericMethod is declared by the DSL but carries its verb in the argument, so it must be matched too",
            genericMethod in SpringWebClasses.ROUTER_DSL_ROUTE_METHODS
        )
        assertNotNull(
            "$genericMethod is no longer declared by $CO_ROUTER_FUNCTION_DSL",
            coRouterFunctionDsl().findMethodsByName(genericMethod, false).firstOrNull()
        )
    }

    /**
     * The rendered set is spelled out here because [com.explyt.spring.web.view.nodes.HttpMethodNode] selects its icon
     * in a `when` over string literals, which no API exposes — this is a deliberate duplicate, not an oversight.
     */
    fun testEveryVerbTheToolWindowCanRenderIsOfferedByTheFilter() {
        val renderedByHttpMethodNode = setOf(
            "CONNECT", "DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT", "TRACE"
        )

        assertEquals(renderedByHttpMethodNode, SpringWebClasses.HTTP_METHOD_FILTER.toSet())
    }

    /**
     * A route method is named after the verb it serves, so it is the only kind of member written in upper case —
     * `nest`, `filter`, `accept` and the rest are excluded by that shape rather than by being listed here.
     */
    private fun routeMethodNamesOf(dslClass: PsiClass): Set<String> = dslClass.methods.asSequence()
        .filter { it.hasModifierProperty(PsiModifier.PUBLIC) }
        .map { it.name }
        .filter { name -> name.all { it.isLetter() && it.isUpperCase() } }
        .toSet()

    private fun coRouterFunctionDsl(): PsiClass {
        val dslClass = LibraryClassCache.searchForLibraryClass(module.project, CO_ROUTER_FUNCTION_DSL)
        assertNotNull("$CO_ROUTER_FUNCTION_DSL is absent from the test libraries", dslClass)
        return dslClass!!
    }
}
