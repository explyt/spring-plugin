/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.test.ExplytJavaLightTestCase
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

/**
 * A controller overriding an interface method that carries the binding annotations - the openapi-generator shape -
 * binds its parameters the way the interface declares them: `HandlerMethod` extends `AnnotatedMethod`, whose
 * `getInheritedParameterAnnotations` adds the parameter annotations of every interface or superclass method the
 * handler overrides, and the media types come from the merged `@RequestMapping` of the mapping source.
 */
class InterfaceParameterAnnotationsTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/example/AppDto.java", "package com.example;\n\npublic class AppDto { public String name; }\n"
        )
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

    /** "Add endpoint to OpenAPI" and the endpoint gutters describe the handler through [SpringWebUtil.getEndpointInfo]. */
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

    /** `AnnotatedMethodParameter` keeps the handler's own annotation and adds an inherited one only of another type. */
    fun testAnnotationOnTheOverrideWinsOverTheInterfaceAnnotation() {
        addAppApi()
        myFixture.addFileToProject(
            "com/example/AppController.java", """
            package com.example;

            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RequestHeader;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class AppController implements AppApi {
                @Override
                public ResponseEntity<AppDto> getApp(@PathVariable("appId") String appId,
                                                     @RequestParam(name = "mode", required = true) String view,
                                                     @RequestHeader("X-Org") String tenant) {
                    return ResponseEntity.ok().build();
                }

                @Override
                public ResponseEntity<AppDto> create(AppDto body) { return ResponseEntity.ok(body); }
            }
            """.trimIndent()
        )

        val handler = handlerOf("/apps/{appId}")

        assertEquals(listOf("mode" to true), SpringWebUtil.collectRequestParameters(handler).map { it.name to it.isRequired })
        assertEquals(listOf("X-Org"), SpringWebUtil.collectRequestHeaders(handler).map { it.name })
    }

    fun testOrdinaryControllerParametersAreUnchanged() {
        myFixture.addFileToProject(
            "com/example/PlainController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RequestHeader;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class PlainController {
                @GetMapping("/plain/{id}")
                public String get(@PathVariable String id, @RequestParam(required = false) String view,
                                  @RequestHeader("X-Tenant") String tenant) { return id; }

                @PostMapping(value = "/plain", consumes = "application/json", produces = "application/json")
                public AppDto create(@RequestBody AppDto body) { return body; }
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

    private fun addAppApiAndController() {
        addAppApi()
        myFixture.addFileToProject(
            "com/example/AppController.java", """
            package com.example;

            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class AppController implements AppApi {
                @Override
                public ResponseEntity<AppDto> getApp(String appId, String view, String tenant) {
                    return ResponseEntity.ok().build();
                }

                @Override
                public ResponseEntity<AppDto> create(AppDto body) { return ResponseEntity.ok(body); }
            }
            """.trimIndent()
        )
    }

    private fun addAppApi() {
        myFixture.addFileToProject(
            "com/example/AppApi.java", """
            package com.example;

            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RequestHeader;
            import org.springframework.web.bind.annotation.RequestParam;

            public interface AppApi {
                @GetMapping("/apps/{appId}")
                ResponseEntity<?> getApp(@PathVariable("appId") String appId,
                                         @RequestParam(required = false) String view,
                                         @RequestHeader("X-Tenant") String tenant);

                @PostMapping(value = "/apps", consumes = "application/json", produces = "application/json")
                ResponseEntity<?> create(@RequestBody AppDto body);
            }
            """.trimIndent()
        )
    }

    /** The method the endpoint model publishes for [path], asserted to be the controller's own declaration. */
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
