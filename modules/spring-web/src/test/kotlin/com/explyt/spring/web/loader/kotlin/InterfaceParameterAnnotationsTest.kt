/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

class InterfaceParameterAnnotationsTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject("com/example/AppDto.kt", "package com.example\n\nclass AppDto(val name: String)\n")
    }

    fun testPathVariableDeclaredOnTheInterfaceBinds() {
        addAppApiAndController()

        val handler = handlerOf("/apps/{appId}")

        assertEquals(listOf("appId" to true), SpringWebUtil.collectPathVariables(handler).map { it.name to it.isRequired })
    }

    fun testRequestParamDeclaredOnTheInterfaceBinds() {
        addAppApiAndController()

        val handler = handlerOf("/apps/{appId}")

        assertEquals(listOf("view" to false), SpringWebUtil.collectRequestParameters(handler).map { it.name to it.isRequired })
    }

    fun testRequestHeaderDeclaredOnTheInterfaceBinds() {
        addAppApiAndController()

        val handler = handlerOf("/apps/{appId}")

        assertEquals(listOf("X-Tenant"), SpringWebUtil.collectRequestHeaders(handler).map { it.name })
    }

    fun testRequestBodyDeclaredOnTheInterfaceBinds() {
        addAppApiAndController()

        val body = SpringWebUtil.getRequestBodyInfo(handlerOf("/apps", "POST"))

        assertNotNull("the body declared on the interface parameter binds", body)
        assertEquals("com.example.AppDto", (body!!.psiElement as PsiParameter).type.canonicalText)
    }

    fun testOpenApiDescriptionOfTheOverrideCarriesTheInterfaceContract() {
        addAppApiAndController()

        val info = endpointInfoOf(handlerOf("/apps", "POST"))

        assertEquals(listOf("application/json"), info.consumes.toList())
        assertEquals(listOf("application/json"), info.produces.toList())
        assertEquals("com.example.AppDto", (info.requestBodyInfo?.psiElement as? PsiParameter)?.type?.canonicalText)
    }

    fun testOpenApiDescriptionOfTheOverrideListsTheInterfaceParameters() {
        addAppApiAndController()

        val info = endpointInfoOf(handlerOf("/apps/{appId}"))

        assertEquals(listOf("appId"), info.pathVariables.map { it.name })
        assertEquals(listOf("view"), info.requestParameters.map { it.name })
        assertEquals(listOf("X-Tenant"), info.requestHeaders.map { it.name })
    }

    fun testAnnotationOnTheOverrideWinsOverTheInterfaceAnnotation() {
        addAppApi()
        myFixture.addFileToProject(
            "com/example/AppController.kt", """
            package com.example

            import org.springframework.http.ResponseEntity
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RequestHeader
            import org.springframework.web.bind.annotation.RequestParam
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class AppController : AppApi {
                override fun getApp(
                    @PathVariable("appId") appId: String,
                    @RequestParam(name = "mode", required = true) view: String,
                    @RequestHeader("X-Org") tenant: String,
                ): ResponseEntity<AppDto> = ResponseEntity.ok().build()

                override fun create(body: AppDto): ResponseEntity<AppDto> = ResponseEntity.ok(body)
            }
            """.trimIndent()
        )

        val handler = handlerOf("/apps/{appId}")

        assertEquals(listOf("mode" to true), SpringWebUtil.collectRequestParameters(handler).map { it.name to it.isRequired })
        assertEquals(listOf("X-Org"), SpringWebUtil.collectRequestHeaders(handler).map { it.name })
    }

    fun testOrdinaryControllerParametersAreUnchanged() {
        myFixture.addFileToProject(
            "com/example/PlainController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.PostMapping
            import org.springframework.web.bind.annotation.RequestBody
            import org.springframework.web.bind.annotation.RequestHeader
            import org.springframework.web.bind.annotation.RequestParam
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class PlainController {
                @GetMapping("/plain/{id}")
                fun get(
                    @PathVariable id: String,
                    @RequestParam(required = false) view: String?,
                    @RequestHeader("X-Tenant") tenant: String,
                ): String = id

                @PostMapping(value = ["/plain"], consumes = ["application/json"], produces = ["application/json"])
                fun create(@RequestBody body: AppDto): AppDto = body
            }
            """.trimIndent()
        )

        val get = handlerOf("/plain/{id}", controller = "PlainController")
        assertEquals(listOf("id"), SpringWebUtil.collectPathVariables(get).map { it.name })
        assertEquals(listOf("view" to false), SpringWebUtil.collectRequestParameters(get).map { it.name to it.isRequired })
        assertEquals(listOf("X-Tenant"), SpringWebUtil.collectRequestHeaders(get).map { it.name })

        val create = endpointInfoOf(handlerOf("/plain", "POST", controller = "PlainController"))
        assertEquals(listOf("application/json"), create.consumes.toList())
        assertEquals(listOf("application/json"), create.produces.toList())
        assertEquals("com.example.AppDto", (create.requestBodyInfo?.psiElement as? PsiParameter)?.type?.canonicalText)
    }

    fun testOverrideBindingNamesReplaceInterfaceBindingsWithoutDuplicates() {
        myFixture.addFileToProject(
            "com/example/BindingController.kt", """
            package com.example
            import org.springframework.web.bind.annotation.*
            interface BindingApi {
                @GetMapping("/bindings/{id}")
                fun get(@PathVariable("interfaceId") id: String, @RequestParam("view") view: String): String
            }
            @RestController
            class BindingController : BindingApi {
                override fun get(@PathVariable("id") id: String, @RequestParam("v") view: String): String = id
            }
            """.trimIndent()
        )

        val handler = handlerOf("/bindings/{id}", controller = "BindingController")
        assertEquals(listOf("id"), SpringWebUtil.collectPathVariables(handler).map { it.name })
        assertEquals(listOf("v"), SpringWebUtil.collectRequestParameters(handler).map { it.name })
    }

    fun testSuperclassParameterAnnotationsBindTheUnannotatedOverride() {
        myFixture.addFileToProject(
            "com/example/SuperController.kt", """
            package com.example
            import org.springframework.web.bind.annotation.*
            abstract class BaseBinding {
                @GetMapping("/super/{id}")
                abstract fun get(@PathVariable("id") id: String, @RequestParam view: String): String
            }
            @RestController
            class SuperController : BaseBinding() {
                override fun get(id: String, view: String): String = id
            }
            """.trimIndent()
        )

        val handler = handlerOf("/super/{id}", controller = "SuperController")
        assertEquals(listOf("id"), SpringWebUtil.collectPathVariables(handler).map { it.name })
        assertEquals(listOf("view"), SpringWebUtil.collectRequestParameters(handler).map { it.name })
        assertEquals(emptyList<String>(), SpringWebUtil.collectRequestHeaders(handler).map { it.name })
    }

    fun testInterfaceAnnotationsAcrossAnAbstractSuperclassBindTheOverride() {
        myFixture.addFileToProject(
            "com/example/ChainController.kt", """
            package com.example
            import org.springframework.web.bind.annotation.*
            interface ChainApi {
                @GetMapping("/chain/{id}")
                fun get(@PathVariable("id") id: String, @RequestParam("view") view: String): String
            }
            abstract class AbstractBinding : ChainApi {
                abstract override fun get(id: String, view: String): String
            }
            @RestController
            class ChainController : AbstractBinding() {
                override fun get(id: String, view: String): String = id
            }
            """.trimIndent()
        )

        val handler = handlerOf("/chain/{id}", controller = "ChainController")
        assertEquals(listOf("id"), SpringWebUtil.collectPathVariables(handler).map { it.name })
        assertEquals(listOf("view"), SpringWebUtil.collectRequestParameters(handler).map { it.name })
    }

    fun testUnmappedMethodWithoutSuperMethodHasNoEndpointOrBindings() {
        myFixture.addFileToProject(
            "com/example/UnmappedController.kt", """
            package com.example
            import org.springframework.web.bind.annotation.RestController
            @RestController
            class UnmappedController {
                fun helper(value: String): String = value
            }
            """.trimIndent()
        )
        val handler = myFixture.findClass("com.example.UnmappedController").findMethodsByName("helper", false).single()
        assertNull(SpringWebUtil.getEndpointInfo(handler.toUElement() as UMethod))
        assertEquals(emptyList<String>(), SpringWebUtil.collectRequestParameters(handler).map { it.name })
        assertEquals(emptyList<String>(), SpringWebUtil.collectPathVariables(handler).map { it.name })
        assertNull(SpringWebUtil.getRequestBodyInfo(handler))
    }

    private fun addAppApiAndController() {
        addAppApi()
        myFixture.addFileToProject(
            "com/example/AppController.kt", """
            package com.example

            import org.springframework.http.ResponseEntity
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class AppController : AppApi {
                override fun getApp(appId: String, view: String?, tenant: String): ResponseEntity<AppDto> =
                    ResponseEntity.ok().build()

                override fun create(body: AppDto): ResponseEntity<AppDto> = ResponseEntity.ok(body)
            }
            """.trimIndent()
        )
    }

    private fun addAppApi() {
        myFixture.addFileToProject(
            "com/example/AppApi.kt", """
            package com.example

            import org.springframework.http.ResponseEntity
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.PostMapping
            import org.springframework.web.bind.annotation.RequestBody
            import org.springframework.web.bind.annotation.RequestHeader
            import org.springframework.web.bind.annotation.RequestParam

            interface AppApi {
                @GetMapping("/apps/{appId}")
                fun getApp(
                    @PathVariable("appId") appId: String,
                    @RequestParam(required = false) view: String?,
                    @RequestHeader("X-Tenant") tenant: String,
                ): ResponseEntity<*>

                @PostMapping(value = ["/apps"], consumes = ["application/json"], produces = ["application/json"])
                fun create(@RequestBody body: AppDto): ResponseEntity<*>
            }
            """.trimIndent()
        )
    }

    private fun handlerOf(path: String, verb: String = "GET", controller: String = "AppController"): PsiMethod {
        val endpoint: EndpointElement = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .singleOrNull { it.type == EndpointType.SPRING_MVC && it.path == path && verb in it.requestMethods }
            ?: error("Precondition: one $verb $path endpoint in the model")
        val handler = endpoint.psiElement as? PsiMethod ?: error("Precondition: the endpoint element is a method")
        assertEquals("Precondition: the handler is declared by $controller", controller, handler.containingClass?.name)
        return handler
    }

    private fun endpointInfoOf(handler: PsiMethod): EndpointInfo =
        SpringWebUtil.getEndpointInfo(handler.toUElement() as UMethod)
            ?: error("the override of ${handler.name} is described as no endpoint at all")
}
