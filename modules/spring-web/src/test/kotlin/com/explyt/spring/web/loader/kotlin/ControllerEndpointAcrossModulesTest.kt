/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.openapi.module.Module

/**
 * A controller is one endpoint however many modules see it, and one endpoint per controller however many controllers
 * inherit its mapping from a base class another module declares.
 */
class ControllerEndpointAcrossModulesTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testControllerSeenFromADependentModuleIsOneEndpoint() {
        val library = addDependencyModule("library")
        addFileToModule(
            library, "com/example/library/LibraryController.kt", """
            package com.example.library

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class LibraryController {
                @GetMapping("/api/library")
                fun get() = "library"
            }
            """.trimIndent()
        )

        assertEquals("the library lists its controller", 1, moduleEndpointsAt(library, "/api/library").size)
        assertEquals("the dependent module lists the controller too", 1, moduleEndpointsAt(module, "/api/library").size)

        val endpoints = projectEndpointsAt("/api/library")
        assertEquals("one endpoint for one controller: ${describe(endpoints)}", 1, endpoints.size)
    }

    fun testEveryControllerInheritingALibraryMappingIsListed() {
        val library = addDependencyModule("library")
        addFileToModule(
            library, "com/example/library/BaseRouteController.kt", """
            package com.example.library

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable

            open class BaseRouteController {
                @GetMapping("/api/base/{id}")
                fun get(@PathVariable id: String) = id
            }
            """.trimIndent()
        )
        addController("RouteViaBase")
        addController("ProbeViaBase")

        val listedByModule = moduleEndpointsAt(module, "/api/base/{id}")
        assertEquals("the module lists the mapping once per controller: ${describe(listedByModule)}", 2, listedByModule.size)

        val endpoints = projectEndpointsAt("/api/base/{id}")
        assertEquals("one endpoint per controller: ${describe(endpoints)}", 2, endpoints.size)
        assertEquals(setOf("RouteViaBase", "ProbeViaBase"), endpoints.map { it.containingClass?.name }.toSet())
    }

    private fun addController(name: String) {
        addFileToModule(
            module, "com/example/app/$name.kt", """
            package com.example.app

            import com.example.library.BaseRouteController
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class $name : BaseRouteController()
            """.trimIndent()
        )
    }

    private fun moduleEndpointsAt(target: Module, path: String): List<EndpointElement> =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(target).filter { it.path == path }

    private fun projectEndpointsAt(path: String): List<EndpointElement> =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints().filter { it.path == path }

    private fun describe(endpoints: List<EndpointElement>) =
        endpoints.map { "${it.requestMethods} ${it.path} ${it.containingClass?.qualifiedName}" }
}
