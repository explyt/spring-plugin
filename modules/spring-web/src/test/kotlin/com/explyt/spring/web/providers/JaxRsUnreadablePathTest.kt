/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.util.SpringWebUtil
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType

class JaxRsUnreadablePathTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    override fun setUp() {
        super.setUp()
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
        myFixture.addFileToProject(
            "com/example/ApiResource.java", """
            package com.example;

            import jakarta.ws.rs.GET;
            import jakarta.ws.rs.Path;

            @Path("/api")
            public class ApiResource {
                @GET
                @Path(Missing.PATH)
                public String unresolved() { return ""; }

                @GET
                @Path("/x")
                public String literal() { return ""; }
            }
            """.trimIndent()
        )
    }

    fun testUnreadableMethodPathReadsAsNoPath() {
        assertEquals(emptyList<String>(), SpringWebUtil.getJaxRsPaths(method("unresolved").javaPsi, module))
    }

    fun testUnreadableMethodPathHasNoEndpointInfo() {
        assertEquals(listOf("/api/x"), listOfNotNull(JaxRsEndpointActionsLineMarkerProvider.getEndpointInfo(method("literal"))?.path))
        assertNull(JaxRsEndpointActionsLineMarkerProvider.getEndpointInfo(method("unresolved"))?.path)
    }

    private fun method(name: String): UMethod =
        myFixture.findClass("com.example.ApiResource").findMethodsByName(name, false).single().toUElementOfType<UMethod>()!!

    private fun jaxRsAnnotation(name: String, declaration: String) {
        myFixture.addFileToProject(
            "jakarta/ws/rs/$name.java",
            "package jakarta.ws.rs;\n\nimport java.lang.annotation.*;\n\n$declaration\n"
        )
    }
}
