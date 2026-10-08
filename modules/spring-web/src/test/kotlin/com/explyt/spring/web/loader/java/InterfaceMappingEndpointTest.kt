/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

/**
 * A controller implementing an interface that declares the request mappings — the shape openapi-generator produces —
 * is served by the controller's override: Spring selects handler methods with `MethodIntrospector.selectMethods`,
 * which keys every mapped method by `ClassUtils.getMostSpecificMethod(method, controller)`, and reads the mapping with
 * `MergedAnnotations` over the `TYPE_HIERARCHY`, where the override's own annotation comes first.
 */
class InterfaceMappingEndpointTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testDefaultInterfaceMappingIsServedByTheUnannotatedOverride() {
        addAppApi(prefix = null, body = "{ return ResponseEntity.ok().build(); }", modifier = "default ")
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
        addAppApi(prefix = null, body = ";", modifier = "")
        addAppController(overrideMapping = null)

        val endpoint = endpointsOf(APP_CONTROLLER).single()

        assertEquals(DELETE_PATH, endpoint.path)
        assertHandledBy(APP_CONTROLLER, endpoint)
    }

    fun testMappingOnInterfaceAndOverrideIsOneEndpoint() {
        addAppApi(prefix = null, body = ";", modifier = "")
        addAppController(overrideMapping = "@DeleteMapping(\"$DELETE_PATH\")")

        val endpoints = endpointsOf(APP_CONTROLLER)

        assertEquals("one handler per path and verb: ${describe(endpoints)}", 1, endpoints.size)
        assertHandledBy(APP_CONTROLLER, endpoints.single())
    }

    fun testOverrideMappingWinsOverTheInterfaceMapping() {
        addAppApi(prefix = null, body = ";", modifier = "")
        addAppController(overrideMapping = "@DeleteMapping(\"/v2/apps/{appId}\")")

        val endpoints = endpointsOf(APP_CONTROLLER)

        assertEquals("only the override's mapping is registered: ${describe(endpoints)}", 1, endpoints.size)
        assertEquals("/v2/apps/{appId}", endpoints.single().path)
        assertHandledBy(APP_CONTROLLER, endpoints.single())
    }

    fun testInterfaceTypeLevelMappingPrefixesTheOverride() {
        addAppApi(prefix = "/v1", body = ";", modifier = "")
        addAppController(overrideMapping = null)

        val endpoint = endpointsOf(APP_CONTROLLER).single()

        assertEquals("/v1$DELETE_PATH", endpoint.path)
        assertHandledBy(APP_CONTROLLER, endpoint)
    }

    fun testInheritedSuperclassMappingWithoutOverrideStaysTheBaseMethod() {
        myFixture.addFileToProject(
            "com/example/BaseRouteController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;

            public class BaseRouteController {
                @GetMapping("/api/base/{id}")
                public String get(@PathVariable String id) { return id; }
            }
            """.trimIndent()
        )
        for (name in listOf("RouteViaBase", "ProbeViaBase")) {
            myFixture.addFileToProject(
                "com/example/$name.java", """
                package com.example;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class $name extends BaseRouteController {}
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
            "com/example/PlainController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class PlainController {
                @GetMapping("/plain/{id}")
                public String get(@PathVariable String id) { return id; }
            }
            """.trimIndent()
        )

        val endpoint = endpointsOf("PlainController").single()

        assertEquals("/plain/{id}", endpoint.path)
        assertEquals(listOf("GET"), endpoint.requestMethods)
        assertHandledBy("PlainController", endpoint)
    }

    /**
     * JAX-RS inherits method annotations from an implemented interface (JAX-RS 3.1, section 3.6): the resource class
     * serves the interface's `@GET`. The annotations are declared as project sources, the loader looks them up by name.
     */
    fun testJaxRsInterfaceMethodIsServedByTheResourceClass() {
        addJaxRsAnnotations()
        myFixture.addFileToProject(
            "com/example/AppResourceApi.java", """
            package com.example;

            import jakarta.ws.rs.GET;
            import jakarta.ws.rs.Path;
            import jakarta.ws.rs.PathParam;

            public interface AppResourceApi {
                @GET
                @Path("$DELETE_PATH")
                String findApp(@PathParam("appId") String appId);
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/AppResource.java", """
            package com.example;

            import jakarta.ws.rs.Path;

            @Path("/jaxrs")
            public class AppResource implements AppResourceApi {
                @Override
                public String findApp(String appId) { return appId; }
            }
            """.trimIndent()
        )
        assertNotNull(
            "Precondition: the resource class is in the fixture",
            JavaPsiFacade.getInstance(project).findClass("com.example.AppResource", GlobalSearchScope.allScope(project))
        )

        val endpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.type == EndpointType.SPRING_JAX_RS }

        assertEquals("one resource method: ${describe(endpoints)}", 1, endpoints.size)
        assertEquals("/jaxrs$DELETE_PATH", endpoints.single().path)
        assertEquals(listOf("GET"), endpoints.single().requestMethods)
        assertHandledBy("AppResource", endpoints.single())
    }

    fun testJaxRsInterfaceWithoutImplementationRemainsDiscoverable() {
        addJaxRsAnnotations()
        myFixture.addFileToProject(
            "com/example/UnimplementedResource.java", """
            package com.example;
            import jakarta.ws.rs.GET;
            import jakarta.ws.rs.Path;
            @Path("/unimplemented")
            public interface UnimplementedResource {
                @GET
                @Path("/apps")
                String get();
            }
            """.trimIndent()
        )
        val resource = myFixture.findClass("com.example.UnimplementedResource")
        val endpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.type == EndpointType.SPRING_JAX_RS }
        assertEquals(1, endpoints.size)
        assertEquals("/unimplemented/apps", endpoints.single().path)
        assertEquals(listOf("GET"), endpoints.single().requestMethods)
        assertEquals(resource, endpoints.single().containingClass)
        assertEquals(resource.findMethodsByName("get", false).single(), endpoints.single().psiElement)
    }

    fun testJaxRsConcreteResourceKeepsItsOwnHandler() {
        addJaxRsAnnotations()
        myFixture.addFileToProject(
            "com/example/ConcreteResource.java", """
            package com.example;
            import jakarta.ws.rs.GET;
            import jakarta.ws.rs.Path;
            @Path("/concrete")
            public class ConcreteResource {
                @GET
                @Path("/apps")
                public String get() { return "apps"; }
            }
            """.trimIndent()
        )
        val resource = myFixture.findClass("com.example.ConcreteResource")
        val endpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.type == EndpointType.SPRING_JAX_RS }
        assertEquals(1, endpoints.size)
        assertEquals("/concrete/apps", endpoints.single().path)
        assertEquals(listOf("GET"), endpoints.single().requestMethods)
        assertEquals(resource, endpoints.single().containingClass)
        assertEquals(resource.findMethodsByName("get", false).single(), endpoints.single().psiElement)
    }

    private fun addJaxRsAnnotations() {
        jaxRsAnnotation(
            "HttpMethod",
            "@Target(ElementType.ANNOTATION_TYPE) @Retention(RetentionPolicy.RUNTIME) public @interface HttpMethod { String value(); }"
        )
        jaxRsAnnotation(
            "Path",
            "@Target({ElementType.TYPE, ElementType.METHOD}) @Retention(RetentionPolicy.RUNTIME) public @interface Path { String value(); }"
        )
        jaxRsAnnotation(
            "GET",
            "@Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME) @HttpMethod(\"GET\") public @interface GET {}"
        )
        jaxRsAnnotation(
            "PathParam",
            "@Target(ElementType.PARAMETER) @Retention(RetentionPolicy.RUNTIME) public @interface PathParam { String value(); }"
        )
    }

    private fun jaxRsAnnotation(name: String, declaration: String) {
        myFixture.addFileToProject(
            "jakarta/ws/rs/$name.java",
            "package jakarta.ws.rs;\n\nimport java.lang.annotation.*;\n\n$declaration\n"
        )
    }

    private fun addAppApi(prefix: String?, body: String, modifier: String) {
        val typeMapping = prefix?.let { "@RequestMapping(\"$it\")\n" } ?: ""
        myFixture.addFileToProject(
            "com/example/AppApi.java", """
            package com.example;

            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.DeleteMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RequestMapping;

            ${typeMapping}public interface AppApi {
                @DeleteMapping("$DELETE_PATH")
                ${modifier}ResponseEntity<?> deleteApp(@PathVariable("appId") String appId) $body
            }
            """.trimIndent()
        )
    }

    private fun addAppController(overrideMapping: String?) {
        myFixture.addFileToProject(
            "com/example/$APP_CONTROLLER.java", """
            package com.example;

            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.DeleteMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class $APP_CONTROLLER implements AppApi {
                @Override
                ${overrideMapping ?: ""}
                public ResponseEntity<Void> deleteApp(String appId) { return ResponseEntity.noContent().build(); }
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
