/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiMethod

class JaxRsInterfaceAcrossModulesTest : ExplytMultiModuleTestCase() {

    fun testInterfaceImplementedInDependentModuleIsListedOnceAtTheImplementation() {
        val api = addDependencyModule("api")
        addJaxRsAnnotations(api)
        addFileToModule(
            api, "com/example/api/AppResourceApi.java", """
            package com.example.api;

            import jakarta.ws.rs.GET;
            import jakarta.ws.rs.Path;

            @Path("/apps")
            public interface AppResourceApi {
                @GET
                @Path("/{appId}")
                String findApp(String appId);
            }
            """.trimIndent()
        )
        addFileToModule(
            module, "com/example/impl/AppResource.java", """
            package com.example.impl;

            import com.example.api.AppResourceApi;

            public class AppResource implements AppResourceApi {
                @Override
                public String findApp(String appId) { return appId; }
            }
            """.trimIndent()
        )
        val apiInterface = myFixture.findClass("com.example.api.AppResourceApi")
        val resource = myFixture.findClass("com.example.impl.AppResource")
        assertEquals("Precondition: the interface is in the api module", api, ModuleUtilCore.findModuleForPsiElement(apiInterface))
        assertEquals("Precondition: the resource is in the impl module", module, ModuleUtilCore.findModuleForPsiElement(resource))

        val endpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()
            .filter { it.type == EndpointType.SPRING_JAX_RS && it.path == "/apps/{appId}" }

        assertEquals("one endpoint for one resource method: ${describe(endpoints)}", 1, endpoints.size)
        assertEquals(
            "the endpoint is served by the resource class: ${describe(endpoints)}",
            resource.findMethodsByName("findApp", false).single(),
            endpoints.single().psiElement
        )
    }

    fun testDependencySourcePathAnnotationsAreRead() {
        val resource = dependencyAnnotatedResource()
        assertEquals(listOf("/apps"), com.explyt.spring.web.util.SpringWebUtil.getJaxRsPaths(resource, module))
    }

    fun testDependencySourceHttpMethodAnnotationsAreRead() {
        val method = dependencyAnnotatedResource().findMethodsByName("create", false).single()
        assertEquals(listOf("POST"), com.explyt.spring.web.util.SpringWebUtil.getJaxRsHttpMethods(method, module))
    }

    fun testDependencySourceProducesAnnotationsAreRead() {
        val method = dependencyAnnotatedResource().findMethodsByName("create", false).single()
        assertEquals(listOf("application/json"), com.explyt.spring.web.util.SpringWebUtil.getJaxRsProduces(method, module))
    }

    fun testDependencySourceConsumesAnnotationsAreRead() {
        val method = dependencyAnnotatedResource().findMethodsByName("create", false).single()
        assertEquals(listOf("application/json"), com.explyt.spring.web.util.SpringWebUtil.getJaxRsConsumes(method, module))
    }

    private fun dependencyAnnotatedResource(): com.intellij.psi.PsiClass {
        val api = addDependencyModule("api")
        addJaxRsAnnotations(api)
        jaxRsAnnotation(api, "POST", "@Target(ElementType.METHOD) @HttpMethod(\"POST\") public @interface POST {}")
        for (name in listOf("Produces", "Consumes")) {
            jaxRsAnnotation(api, name, "@Target({ElementType.TYPE, ElementType.METHOD}) public @interface $name { String[] value(); }")
        }
        addFileToModule(module, "com/example/impl/LocalResource.java", """
            package com.example.impl;
            import jakarta.ws.rs.*;
            @Path("/apps")
            public class LocalResource {
                @POST
                @Produces("application/json")
                @Consumes("application/json")
                public String create() { return "apps"; }
            }
            """.trimIndent())
        val resource = myFixture.findClass("com.example.impl.LocalResource")
        assertEquals(module, ModuleUtilCore.findModuleForPsiElement(resource))
        for (name in listOf("Path", "HttpMethod", "POST", "Produces", "Consumes")) {
            assertEquals("Precondition: $name is a dependency source", api,
                ModuleUtilCore.findModuleForPsiElement(myFixture.findClass("jakarta.ws.rs.$name")))
        }
        assertNotNull(resource.getAnnotation("jakarta.ws.rs.Path"))
        assertNotNull(resource.findMethodsByName("create", false).single().getAnnotation("jakarta.ws.rs.POST"))
        return resource
    }

    private fun addJaxRsAnnotations(target: Module) {
        jaxRsAnnotation(
            target, "HttpMethod",
            "@Target(ElementType.ANNOTATION_TYPE) @Retention(RetentionPolicy.RUNTIME) public @interface HttpMethod { String value(); }"
        )
        jaxRsAnnotation(
            target, "Path",
            "@Target({ElementType.TYPE, ElementType.METHOD}) @Retention(RetentionPolicy.RUNTIME) public @interface Path { String value(); }"
        )
        jaxRsAnnotation(
            target, "GET",
            "@Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME) @HttpMethod(\"GET\") public @interface GET {}"
        )
    }

    private fun jaxRsAnnotation(target: Module, name: String, declaration: String) {
        addFileToModule(
            target, "jakarta/ws/rs/$name.java", "package jakarta.ws.rs;\n\nimport java.lang.annotation.*;\n\n$declaration\n"
        )
    }

    private fun describe(endpoints: List<EndpointElement>) = endpoints.map {
        "${it.requestMethods} ${it.path} ${(it.psiElement as? PsiMethod)?.containingClass?.qualifiedName}"
    }
}
