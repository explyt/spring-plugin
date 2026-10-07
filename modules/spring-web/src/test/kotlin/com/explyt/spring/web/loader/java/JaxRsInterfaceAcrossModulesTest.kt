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
