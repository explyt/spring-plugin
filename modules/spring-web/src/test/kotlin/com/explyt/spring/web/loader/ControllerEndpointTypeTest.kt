/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.view.EndpointsTreeData
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMember

/**
 * The endpoint type an annotated controller is listed under, by the web stack its application runs on.
 *
 * `@RestController` and `@GetMapping` are the same annotations on both stacks, so the controller says nothing about
 * the stack; the classpath does, the way Spring Boot's `WebApplicationType.deduce()` reads it. One class per classpath,
 * because a light fixture fixes its libraries per test class.
 */
abstract class ControllerEndpointTypeTestCase : ExplytJavaLightTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/example/OrderController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            @RequestMapping("/api/orders")
            public class OrderController {
                @GetMapping("/status") public String status(@RequestParam String id) { return id; }
            }
            """.trimIndent()
        )
    }

    protected fun assertDispatchers(servlet: Boolean, reactive: Boolean) {
        val facade = JavaPsiFacade.getInstance(project)
        val scope = module.moduleWithLibrariesScope
        assertEquals(
            "DispatcherServlet on the classpath", servlet,
            facade.findClass("org.springframework.web.servlet.DispatcherServlet", scope) != null
        )
        assertEquals(
            "DispatcherHandler on the classpath", reactive,
            facade.findClass("org.springframework.web.reactive.DispatcherHandler", scope) != null
        )
    }

    protected fun controllerTypes(): List<EndpointType> =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.containingClass?.name == "OrderController" }
            .map { it.type }
            .distinct()
}

class ControllerEndpointTypeReactiveTest : ControllerEndpointTypeTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springReactiveWeb_3_1_1)

    fun testAnnotatedControllerOfAReactiveApplicationIsAWebFluxEndpoint() {
        assertDispatchers(servlet = false, reactive = true)

        assertEquals(listOf(EndpointType.SPRING_WEBFLUX), controllerTypes())
    }

    fun testToolWindowListsTheControllerUnderWebFlux() {
        assertDispatchers(servlet = false, reactive = true)

        val rows = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .flatMap(EndpointsTreeData::rowsOf)
        val tree = EndpointsTreeData.byType(rows)
            .associate { byType -> byType.type to byType.list.map { it.classOrFileName } }

        assertEquals(listOf("OrderController"), tree[EndpointType.SPRING_WEBFLUX])
        assertNull("no Spring MVC group in a reactive application, got $tree", tree[EndpointType.SPRING_MVC])
    }

    /** A URL in code still resolves to the handler: the reference asks for both stacks. */
    fun testUrlReferenceStillResolvesToTheHandler() {
        myFixture.configureByText(
            "OrderClient.java", """
            import org.springframework.web.reactive.function.client.WebClient;

            class OrderClient {
                void send(WebClient client) {
                    client.get().uri("/api/orders/sta<caret>tus");
                }
            }
            """.trimIndent()
        )

        val reference = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull("a URL reference on the literal", reference)
        val resolved = reference!!.multiResolve(false).map { (it.element as PsiMember).name }

        assertEquals(listOf("status"), resolved)
    }
}

class ControllerEndpointTypeServletTest : ControllerEndpointTypeTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testAnnotatedControllerOfAServletApplicationIsASpringMvcEndpoint() {
        assertDispatchers(servlet = true, reactive = false)

        assertEquals(listOf(EndpointType.SPRING_MVC), controllerTypes())
    }
}

/** WebFlux next to servlet MVC is usually there for `WebClient`; Spring Boot still starts a servlet application. */
class ControllerEndpointTypeMixedTest : ControllerEndpointTypeTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springWebMvc_6_0_7, TestLibrary.springReactiveWeb_3_1_1)

    fun testAnnotatedControllerOfAMixedClasspathIsASpringMvcEndpoint() {
        assertDispatchers(servlet = true, reactive = true)

        assertEquals(listOf(EndpointType.SPRING_MVC), controllerTypes())
    }
}
