/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.service.SpringWebEndpointsSearcher

/**
 * Spring registers the handler methods of every controller bean, inherited ones included: two controllers extending
 * one base class each serve the mapping the base declares. The endpoint model lists each of them, the way the running
 * application does.
 */
class InheritedMappingEndpointTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testEveryControllerInheritingTheSameMappingIsListed() {
        addBaseRouteController()
        addController("RouteViaBase", prefix = null)
        addController("ProbeViaBase", prefix = null)

        val listedByModule = moduleEndpointsAt("/api/base/{id}")
        assertEquals("the module lists the mapping once per controller: ${describe(listedByModule)}", 2, listedByModule.size)

        val endpoints = projectEndpointsAt("/api/base/{id}")
        assertEquals("one endpoint per controller: ${describe(endpoints)}", 2, endpoints.size)
        assertEquals(setOf("RouteViaBase", "ProbeViaBase"), endpoints.map { it.containingClass?.name }.toSet())
    }

    fun testControllersWithTheirOwnPrefixServeTheirOwnPaths() {
        addBaseRouteController()
        addController("RouteViaBase", prefix = "/a")
        addController("ProbeViaBase", prefix = "/b")

        val endpoints = projectEndpoints().filter { it.containingClass?.name in CONTROLLERS }
        assertEquals(
            setOf("/a/api/base/{id}" to "RouteViaBase", "/b/api/base/{id}" to "ProbeViaBase"),
            endpoints.map { it.path to it.containingClass?.name }.toSet()
        )
    }

    private fun addBaseRouteController() {
        myFixture.addFileToProject(
            "com/example/BaseRouteController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable

            open class BaseRouteController {
                @GetMapping("/api/base/{id}")
                fun get(@PathVariable id: String) = id
            }
            """.trimIndent()
        )
    }

    private fun addController(name: String, prefix: String?) {
        val mapping = prefix?.let { "@RequestMapping(\"$it\")\n" } ?: ""
        myFixture.addFileToProject(
            "com/example/$name.kt", """
            package com.example

            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            ${mapping}class $name : BaseRouteController()
            """.trimIndent()
        )
    }

    private fun moduleEndpointsAt(path: String): List<EndpointElement> =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module).filter { it.path == path }

    private fun projectEndpoints(): List<EndpointElement> =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()

    private fun projectEndpointsAt(path: String): List<EndpointElement> = projectEndpoints().filter { it.path == path }

    private fun describe(endpoints: List<EndpointElement>) =
        endpoints.map { "${it.requestMethods} ${it.path} ${it.containingClass?.name}" }

    private companion object {
        val CONTROLLERS = setOf("RouteViaBase", "ProbeViaBase")
    }
}
