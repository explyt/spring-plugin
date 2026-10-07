/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

/**
 * A controller implementing an interface that declares the request mappings is served by the controller's override:
 * Spring keys every mapped method by `ClassUtils.getMostSpecificMethod(method, controller)` and reads the mapping with
 * `MergedAnnotations` over the `TYPE_HIERARCHY`, where the override's own annotation comes first.
 */
class InterfaceMappingEndpointTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testDefaultInterfaceMappingIsServedByTheUnannotatedOverride() {
        addAppApi(prefix = null, body = " = ResponseEntity.ok().build()")
        addAppController(overrideMapping = null)

        val endpoint = endpointsOf(APP_CONTROLLER).single()

        assertEquals(DELETE_PATH, endpoint.path)
        assertEquals(listOf("DELETE"), endpoint.requestMethods)
        assertHandledBy(APP_CONTROLLER, endpoint)
        assertEquals(
            "the handler declares the response the controller returns",
            "org.springframework.http.ResponseEntity<java.lang.Void>",
            (endpoint.psiElement as PsiMethod).returnType?.canonicalText
        )
    }

    fun testAbstractInterfaceMappingIsServedByTheOverride() {
        addAppApi(prefix = null, body = "")
        addAppController(overrideMapping = null)

        val endpoint = endpointsOf(APP_CONTROLLER).single()

        assertEquals(DELETE_PATH, endpoint.path)
        assertHandledBy(APP_CONTROLLER, endpoint)
    }

    fun testMappingOnInterfaceAndOverrideIsOneEndpoint() {
        addAppApi(prefix = null, body = "")
        addAppController(overrideMapping = "@DeleteMapping(\"$DELETE_PATH\")")

        val endpoints = endpointsOf(APP_CONTROLLER)

        assertEquals("one handler per path and verb: ${describe(endpoints)}", 1, endpoints.size)
        assertHandledBy(APP_CONTROLLER, endpoints.single())
    }

    fun testOverrideMappingWinsOverTheInterfaceMapping() {
        addAppApi(prefix = null, body = "")
        addAppController(overrideMapping = "@DeleteMapping(\"/v2/apps/{appId}\")")

        val endpoints = endpointsOf(APP_CONTROLLER)

        assertEquals("only the override's mapping is registered: ${describe(endpoints)}", 1, endpoints.size)
        assertEquals("/v2/apps/{appId}", endpoints.single().path)
        assertHandledBy(APP_CONTROLLER, endpoints.single())
    }

    fun testInterfaceTypeLevelMappingPrefixesTheOverride() {
        addAppApi(prefix = "/v1", body = "")
        addAppController(overrideMapping = null)

        val endpoint = endpointsOf(APP_CONTROLLER).single()

        assertEquals("/v1$DELETE_PATH", endpoint.path)
        assertHandledBy(APP_CONTROLLER, endpoint)
    }

    fun testInheritedSuperclassMappingWithoutOverrideStaysTheBaseMethod() {
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
        for (name in listOf("RouteViaBase", "ProbeViaBase")) {
            myFixture.addFileToProject(
                "com/example/$name.kt", """
                package com.example

                import org.springframework.web.bind.annotation.RestController

                @RestController
                class $name : BaseRouteController()
                """.trimIndent()
            )
        }

        val endpoints = endpointsOf("RouteViaBase") + endpointsOf("ProbeViaBase")

        assertEquals("one endpoint per controller: ${describe(endpoints)}", 2, endpoints.size)
        assertEquals(
            setOf("BaseRouteController"),
            endpoints.map { (it.psiElement as PsiMethod).containingClass?.name }.toSet()
        )
    }

    fun testOrdinaryControllerMethodIsItsOwnHandler() {
        myFixture.addFileToProject(
            "com/example/PlainController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class PlainController {
                @GetMapping("/plain/{id}")
                fun get(@PathVariable id: String) = id
            }
            """.trimIndent()
        )

        val endpoint = endpointsOf("PlainController").single()

        assertEquals("/plain/{id}", endpoint.path)
        assertEquals(listOf("GET"), endpoint.requestMethods)
        assertHandledBy("PlainController", endpoint)
    }

    private fun addAppApi(prefix: String?, body: String) {
        val typeMapping = prefix?.let { "@RequestMapping(\"$it\")\n" } ?: ""
        myFixture.addFileToProject(
            "com/example/AppApi.kt", """
            package com.example

            import org.springframework.http.ResponseEntity
            import org.springframework.web.bind.annotation.DeleteMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RequestMapping

            ${typeMapping}interface AppApi {
                @DeleteMapping("$DELETE_PATH")
                fun deleteApp(@PathVariable("appId") appId: String): ResponseEntity<*>$body
            }
            """.trimIndent()
        )
    }

    private fun addAppController(overrideMapping: String?) {
        myFixture.addFileToProject(
            "com/example/$APP_CONTROLLER.kt", """
            package com.example

            import org.springframework.http.ResponseEntity
            import org.springframework.web.bind.annotation.DeleteMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class $APP_CONTROLLER : AppApi {
                ${overrideMapping ?: ""}
                override fun deleteApp(appId: String): ResponseEntity<Void> = ResponseEntity.noContent().build()
            }
            """.trimIndent()
        )
    }

    private fun endpointsOf(controller: String): List<EndpointElement> {
        assertNotNull(
            "Precondition: the controller $controller is in the fixture",
            JavaPsiFacade.getInstance(project).findClass("com.example.$controller", GlobalSearchScope.allScope(project))
        )
        return SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.type == EndpointType.SPRING_MVC && it.containingClass?.name == controller }
    }

    private fun assertHandledBy(controller: String, endpoint: EndpointElement) {
        val handler = endpoint.psiElement as? PsiMethod ?: error("the endpoint element is not a method: ${endpoint.psiElement}")
        assertEquals(
            "the handler is the method Spring invokes, declared by $controller",
            controller,
            handler.containingClass?.name
        )
    }

    private fun describe(endpoints: List<EndpointElement>) = endpoints.map {
        "${it.requestMethods} ${it.path} ${(it.psiElement as? PsiMethod)?.containingClass?.name}"
    }

    private companion object {
        const val APP_CONTROLLER = "AppController"
        const val DELETE_PATH = "/apps/{appId}"
    }
}
